package com.whu.distributed.seckill.seckill.service;

import com.whu.distributed.seckill.common.SnowflakeIdGenerator;
import com.whu.distributed.seckill.mq.OrderMessageProducer;
import com.whu.distributed.seckill.mq.dto.SeckillOrderMessage;
import com.whu.distributed.seckill.product.entity.Product;
import com.whu.distributed.seckill.product.mapper.ProductMapper;
import com.whu.distributed.seckill.seckill.dto.SeckillRequest;
import com.whu.distributed.seckill.seckill.dto.SeckillSubmitResponse;
import com.whu.distributed.seckill.user.entity.User;
import com.whu.distributed.seckill.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SeckillService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SeckillCacheService seckillCacheService;
    private final OrderMessageProducer orderMessageProducer;

    public SeckillService(UserMapper userMapper,
                          ProductMapper productMapper,
                          SnowflakeIdGenerator snowflakeIdGenerator,
                          SeckillCacheService seckillCacheService,
                          OrderMessageProducer orderMessageProducer) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.seckillCacheService = seckillCacheService;
        this.orderMessageProducer = orderMessageProducer;
    }

    public SeckillSubmitResponse submit(SeckillRequest request) {
        validateRequest(request);

        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }

        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("product not found");
        }

        if (!"ON_SALE".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalArgumentException("product is not on sale");
        }

        String orderId = String.valueOf(snowflakeIdGenerator.nextId());
        SeckillOrderMessage message = new SeckillOrderMessage(
                orderId,
                request.getUserId(),
                request.getProductId(),
                request.getQuantity(),
                LocalDateTime.now()
        );

        seckillCacheService.reserveStock(
                request.getUserId(),
                request.getProductId(),
                request.getQuantity(),
                orderId
        );

        try {
            orderMessageProducer.send(message);
        } catch (Exception ex) {
            seckillCacheService.cancelReservation(message, "enqueue order request failed");
            throw ex;
        }

        return new SeckillSubmitResponse(orderId, "PENDING", "request accepted");
    }

    private void validateRequest(SeckillRequest request) {
        if (request == null || request.getUserId() == null || request.getProductId() == null) {
            throw new IllegalArgumentException("userId and productId are required");
        }

        if (request.getUserId() <= 0 || request.getProductId() <= 0) {
            throw new IllegalArgumentException("invalid userId or productId");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
