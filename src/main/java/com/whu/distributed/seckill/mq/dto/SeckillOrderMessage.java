package com.whu.distributed.seckill.mq.dto;

import java.time.LocalDateTime;

public class SeckillOrderMessage {

    private String orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime requestedAt;

    public SeckillOrderMessage() {
    }

    public SeckillOrderMessage(String orderId,
                               Long userId,
                               Long productId,
                               Integer quantity,
                               LocalDateTime requestedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.requestedAt = requestedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
