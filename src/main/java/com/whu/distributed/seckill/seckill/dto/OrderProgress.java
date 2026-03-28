package com.whu.distributed.seckill.seckill.dto;

import java.time.LocalDateTime;

public class OrderProgress {

    private String orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String status;
    private String message;
    private LocalDateTime createdAt;

    public OrderProgress() {
    }

    public OrderProgress(String orderId,
                         Long userId,
                         Long productId,
                         Integer quantity,
                         String status,
                         String message,
                         LocalDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
