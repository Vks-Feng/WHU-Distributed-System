package com.whu.distributed.seckill.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.distributed.seckill.product.dto.ProductDetailResponse;
import com.whu.distributed.seckill.product.entity.Product;
import com.whu.distributed.seckill.product.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String PRODUCT_CACHE_PREFIX = "seckill:product:";
    private static final String PRODUCT_LOCK_PREFIX = "seckill:product:lock:";
    private static final String NULL_MARKER = "__NULL__";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;
    private final long ttlSeconds;
    private final long nullTtlSeconds;
    private final long lockTtlSeconds;
    private final long ttlJitterSeconds;

    public ProductService(ProductMapper productMapper,
                          StringRedisTemplate stringRedisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${app.instance-id:local}") String instanceId,
                          @Value("${cache.product.ttl-seconds:300}") long ttlSeconds,
                          @Value("${cache.product.null-ttl-seconds:60}") long nullTtlSeconds,
                          @Value("${cache.product.lock-ttl-seconds:10}") long lockTtlSeconds,
                          @Value("${cache.product.ttl-jitter-seconds:120}") long ttlJitterSeconds) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.ttlSeconds = ttlSeconds;
        this.nullTtlSeconds = nullTtlSeconds;
        this.lockTtlSeconds = lockTtlSeconds;
        this.ttlJitterSeconds = ttlJitterSeconds;
    }

    public ProductDetailResponse getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("invalid product id");
        }

        String cacheKey = PRODUCT_CACHE_PREFIX + id;
        String cached = getCache(cacheKey);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                throw new IllegalArgumentException("product not found");
            }
            ProductDetailResponse cacheHit = readProduct(cached);
            if (cacheHit != null) {
                cacheHit.setServedBy(instanceId);
                return cacheHit;
            }
        }

        ProductDetailResponse product = loadWithMutex(id, cacheKey);
        if (product == null) {
            throw new IllegalArgumentException("product not found");
        }
        product.setServedBy(instanceId);
        return product;
    }

    public List<ProductDetailResponse> list(int page, int size) {
        if (page <= 0 || size <= 0 || size > 100) {
            throw new IllegalArgumentException("invalid pagination params");
        }

        int offset = (page - 1) * size;
        return productMapper.findPage(offset, size).stream()
                .map(this::toResponse)
                .peek(item -> item.setServedBy(instanceId))
                .toList();
    }

    private ProductDetailResponse loadWithMutex(Long id, String cacheKey) {
        String lockKey = PRODUCT_LOCK_PREFIX + id;
        String token = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, token);

        if (!locked) {
            ProductDetailResponse fromWait = waitAndReadCache(id, cacheKey);
            if (fromWait != null) {
                return fromWait;
            }
            return loadAndCacheFromDb(id, cacheKey);
        }

        try {
            String cached = getCache(cacheKey);
            if (cached != null) {
                if (NULL_MARKER.equals(cached)) {
                    return null;
                }
                ProductDetailResponse cacheHit = readProduct(cached);
                if (cacheHit != null) {
                    return cacheHit;
                }
            }
            return loadAndCacheFromDb(id, cacheKey);
        } finally {
            unlock(lockKey, token);
        }
    }

    private ProductDetailResponse waitAndReadCache(Long id, String cacheKey) {
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(40L * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            String cached = getCache(cacheKey);
            if (!StringUtils.hasText(cached)) {
                continue;
            }
            if (NULL_MARKER.equals(cached)) {
                return null;
            }
            ProductDetailResponse cacheHit = readProduct(cached);
            if (cacheHit != null) {
                return cacheHit;
            }
        }
        return null;
    }

    private ProductDetailResponse loadAndCacheFromDb(Long id, String cacheKey) {
        Product product = productMapper.findById(id);
        if (product == null) {
            setCache(cacheKey, NULL_MARKER, nullTtlSeconds + randomJitter(30));
            return null;
        }

        ProductDetailResponse response = toResponse(product);
        setCache(cacheKey, toJson(response), ttlSeconds + randomJitter(ttlJitterSeconds));
        return response;
    }

    private ProductDetailResponse toResponse(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStatus(),
                instanceId
        );
    }

    private String getCache(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            log.warn("Redis get failed, key={}", key, ex);
            return null;
        }
    }

    private void setCache(String key, String value, long ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
        } catch (Exception ex) {
            log.warn("Redis set failed, key={}", key, ex);
        }
    }

    private boolean tryLock(String lockKey, String token) {
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue()
                            .setIfAbsent(lockKey, token, Duration.ofSeconds(lockTtlSeconds))
            );
        } catch (Exception ex) {
            log.warn("Redis lock failed, lockKey={}", lockKey, ex);
            return false;
        }
    }

    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (Exception ex) {
            log.warn("Redis unlock failed, lockKey={}", lockKey, ex);
        }
    }

    private ProductDetailResponse readProduct(String json) {
        try {
            return objectMapper.readValue(json, ProductDetailResponse.class);
        } catch (Exception ex) {
            log.warn("Parse product cache failed", ex);
            return null;
        }
    }

    private String toJson(ProductDetailResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize product cache failed", ex);
        }
    }

    private long randomJitter(long max) {
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextLong(max + 1);
    }
}
