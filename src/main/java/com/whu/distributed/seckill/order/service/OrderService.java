package com.whu.distributed.seckill.order.service;

import com.whu.distributed.seckill.inventory.mapper.InventoryMapper;
import com.whu.distributed.seckill.mq.dto.SeckillOrderMessage;
import com.whu.distributed.seckill.order.dto.OrderDetailResponse;
import com.whu.distributed.seckill.order.entity.Order;
import com.whu.distributed.seckill.order.mapper.OrderMapper;
import com.whu.distributed.seckill.product.entity.Product;
import com.whu.distributed.seckill.product.mapper.ProductMapper;
import com.whu.distributed.seckill.seckill.dto.OrderProgress;
import com.whu.distributed.seckill.seckill.service.SeckillCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final SeckillCacheService seckillCacheService;

    public OrderService(OrderMapper orderMapper,
                        ProductMapper productMapper,
                        InventoryMapper inventoryMapper,
                        SeckillCacheService seckillCacheService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.seckillCacheService = seckillCacheService;
    }

    @Transactional
    public Order createOrder(SeckillOrderMessage message) {
        Order existed = orderMapper.findByOrderNo(message.getOrderId());
        if (existed != null) {
            return existed;
        }

        Product product = productMapper.findById(message.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("product not found");
        }

        int updated = inventoryMapper.deductAvailableStock(message.getProductId(), message.getQuantity());
        if (updated <= 0) {
            throw new IllegalArgumentException("stock not enough");
        }

        Order order = new Order();
        order.setOrderNo(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setQuantity(message.getQuantity());
        order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setStatus("CREATED");
        orderMapper.insert(order);
        return orderMapper.findByOrderNo(message.getOrderId());
    }

    public OrderDetailResponse getByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("invalid order id");
        }

        Order order = orderMapper.findByOrderNo(orderId.trim());
        if (order != null) {
            return toResponse(order, "order created");
        }

        OrderProgress progress = seckillCacheService.getProgress(orderId.trim());
        if (progress != null) {
            return new OrderDetailResponse(
                    progress.getOrderId(),
                    progress.getUserId(),
                    progress.getProductId(),
                    progress.getQuantity(),
                    null,
                    progress.getStatus(),
                    progress.getMessage(),
                    progress.getCreatedAt(),
                    progress.getCreatedAt()
            );
        }

        throw new IllegalArgumentException("order not found");
    }

    public List<OrderDetailResponse> listByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("invalid user id");
        }

        return orderMapper.findByUserId(userId).stream()
                .map(order -> toResponse(order, "order created"))
                .toList();
    }

    public Order findEntityByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return null;
        }
        return orderMapper.findByOrderNo(orderId.trim());
    }

    public Order findEntityByUserAndProduct(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return null;
        }
        return orderMapper.findByUserAndProduct(userId, productId);
    }

    private OrderDetailResponse toResponse(Order order, String message) {
        return new OrderDetailResponse(
                order.getOrderNo(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus(),
                message,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
