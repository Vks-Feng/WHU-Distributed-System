package com.whu.distributed.seckill.seckill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.distributed.seckill.inventory.entity.Inventory;
import com.whu.distributed.seckill.inventory.mapper.InventoryMapper;
import com.whu.distributed.seckill.mq.dto.SeckillOrderMessage;
import com.whu.distributed.seckill.order.entity.Order;
import com.whu.distributed.seckill.seckill.dto.OrderProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SeckillCacheService {

    private static final Logger log = LoggerFactory.getLogger(SeckillCacheService.class);
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_ORDER_KEY_PREFIX = "seckill:order:user:";
    private static final String ORDER_PROGRESS_KEY_PREFIX = "seckill:order:progress:";
    private static final String STOCK_LOAD_LOCK_KEY_PREFIX = "seckill:stock:load:lock:";

    private static final DefaultRedisScript<Long> RESERVE_STOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('exists', KEYS[2]) == 1 then
                        return 2
                    end
                    local stock = redis.call('get', KEYS[1])
                    if not stock then
                        return -1
                    end
                    stock = tonumber(stock)
                    local quantity = tonumber(ARGV[1])
                    if stock < quantity then
                        return 0
                    end
                    redis.call('decrby', KEYS[1], quantity)
                    redis.call('set', KEYS[2], ARGV[2])
                    redis.call('setex', KEYS[3], tonumber(ARGV[4]), ARGV[3])
                    return 1
                    """,
            Long.class
    );
    private static final DefaultRedisScript<Long> COMPENSATE_STOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[2]) == ARGV[2] then
                        redis.call('incrby', KEYS[1], tonumber(ARGV[1]))
                        redis.call('del', KEYS[2])
                    end
                    redis.call('setex', KEYS[3], tonumber(ARGV[4]), ARGV[3])
                    return 1
                    """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final InventoryMapper inventoryMapper;
    private final long orderProgressTtlHours;
    private final long stockLoadLockSeconds;

    public SeckillCacheService(StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper,
                               InventoryMapper inventoryMapper,
                               @Value("${seckill.order-progress-ttl-hours:24}") long orderProgressTtlHours,
                               @Value("${seckill.stock-load-lock-seconds:5}") long stockLoadLockSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderProgressTtlHours = orderProgressTtlHours;
        this.stockLoadLockSeconds = stockLoadLockSeconds;
    }

    public void reserveStock(Long userId, Long productId, Integer quantity, String orderId) {
        ensureStockLoaded(productId);

        OrderProgress progress = new OrderProgress(
                orderId,
                userId,
                productId,
                quantity,
                "PENDING",
                "request accepted, waiting for async order creation",
                LocalDateTime.now()
        );

        Long result = stringRedisTemplate.execute(
                RESERVE_STOCK_SCRIPT,
                List.of(stockKey(productId), userOrderKey(userId, productId), progressKey(orderId)),
                String.valueOf(quantity),
                orderId,
                writeJson(progress),
                String.valueOf(Duration.ofHours(orderProgressTtlHours).getSeconds())
        );

        if (result == null) {
            throw new IllegalStateException("reserve stock failed");
        }

        if (result == 1L) {
            return;
        }

        if (result == 2L) {
            String existingOrderId = stringRedisTemplate.opsForValue().get(userOrderKey(userId, productId));
            throw new IllegalArgumentException("duplicate seckill request, existing orderId=" + existingOrderId);
        }

        if (result == 0L) {
            throw new IllegalArgumentException("sold out");
        }

        throw new IllegalStateException("stock cache not loaded");
    }

    public void markSuccess(Order order) {
        OrderProgress progress = new OrderProgress(
                order.getOrderNo(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getStatus(),
                "order created",
                order.getCreatedAt()
        );
        saveProgress(progress);
    }

    public void markFailed(SeckillOrderMessage message, String reason) {
        try {
            OrderProgress progress = new OrderProgress(
                    message.getOrderId(),
                    message.getUserId(),
                    message.getProductId(),
                    message.getQuantity(),
                    "FAILED",
                    StringUtils.hasText(reason) ? reason : "async order creation failed",
                    message.getRequestedAt() == null ? LocalDateTime.now() : message.getRequestedAt()
            );
            stringRedisTemplate.execute(
                    COMPENSATE_STOCK_SCRIPT,
                    List.of(
                            stockKey(message.getProductId()),
                            userOrderKey(message.getUserId(), message.getProductId()),
                            progressKey(message.getOrderId())
                    ),
                    String.valueOf(message.getQuantity()),
                    message.getOrderId(),
                    writeJson(progress),
                    String.valueOf(Duration.ofHours(orderProgressTtlHours).getSeconds())
            );
        } catch (Exception ex) {
            log.error("mark order failed state in redis failed, orderId={}", message.getOrderId(), ex);
        }
    }

    public OrderProgress getProgress(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return null;
        }

        try {
            String json = stringRedisTemplate.opsForValue().get(progressKey(orderId));
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, OrderProgress.class);
        } catch (Exception ex) {
            log.warn("read order progress failed, orderId={}", orderId, ex);
            return null;
        }
    }

    public Integer getCachedStock(Long productId) {
        ensureStockLoaded(productId);
        try {
            String stock = stringRedisTemplate.opsForValue().get(stockKey(productId));
            if (!StringUtils.hasText(stock)) {
                return null;
            }
            return Integer.parseInt(stock);
        } catch (Exception ex) {
            log.warn("read redis stock failed, productId={}", productId, ex);
            return null;
        }
    }

    public void cancelReservation(SeckillOrderMessage message, String reason) {
        markFailed(message, reason);
    }

    private void ensureStockLoaded(Long productId) {
        String key = stockKey(productId);
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                return;
            }
        } catch (Exception ex) {
            log.warn("check redis stock key failed, productId={}", productId, ex);
        }

        String lockKey = STOCK_LOAD_LOCK_KEY_PREFIX + productId;
        String token = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, Duration.ofSeconds(stockLoadLockSeconds));

        if (Boolean.TRUE.equals(locked)) {
            try {
                Inventory inventory = inventoryMapper.findByProductId(productId);
                int availableStock = inventory == null || inventory.getAvailableStock() == null
                        ? 0
                        : inventory.getAvailableStock();
                stringRedisTemplate.opsForValue().set(key, String.valueOf(availableStock));
            } finally {
                releaseLock(lockKey, token);
            }
            return;
        }

        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(50L * (i + 1));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            try {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                    return;
                }
            } catch (Exception ex) {
                log.warn("wait redis stock key failed, productId={}", productId, ex);
            }
        }

        Inventory inventory = inventoryMapper.findByProductId(productId);
        int availableStock = inventory == null || inventory.getAvailableStock() == null
                ? 0
                : inventory.getAvailableStock();
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(availableStock));
    }

    private void releaseLock(String lockKey, String token) {
        try {
            String current = stringRedisTemplate.opsForValue().get(lockKey);
            if (token.equals(current)) {
                stringRedisTemplate.delete(lockKey);
            }
        } catch (Exception ex) {
            log.warn("release stock load lock failed, lockKey={}", lockKey, ex);
        }
    }

    private void saveProgress(OrderProgress progress) {
        stringRedisTemplate.opsForValue().set(
                progressKey(progress.getOrderId()),
                writeJson(progress),
                Duration.ofHours(orderProgressTtlHours)
        );
    }

    private String writeJson(OrderProgress progress) {
        try {
            return objectMapper.writeValueAsString(progress);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize order progress failed", ex);
        }
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String userOrderKey(Long userId, Long productId) {
        return USER_ORDER_KEY_PREFIX + userId + ":" + productId;
    }

    private String progressKey(String orderId) {
        return ORDER_PROGRESS_KEY_PREFIX + orderId;
    }
}
