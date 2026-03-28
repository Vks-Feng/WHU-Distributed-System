package com.whu.distributed.seckill.inventory.dto;

import java.time.LocalDateTime;

public class InventoryStockResponse {

    private Long productId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;
    private Integer redisAvailableStock;
    private LocalDateTime updatedAt;

    public InventoryStockResponse() {
    }

    public InventoryStockResponse(Long productId,
                                  Integer totalStock,
                                  Integer availableStock,
                                  Integer lockedStock,
                                  Integer redisAvailableStock,
                                  LocalDateTime updatedAt) {
        this.productId = productId;
        this.totalStock = totalStock;
        this.availableStock = availableStock;
        this.lockedStock = lockedStock;
        this.redisAvailableStock = redisAvailableStock;
        this.updatedAt = updatedAt;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getLockedStock() {
        return lockedStock;
    }

    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }

    public Integer getRedisAvailableStock() {
        return redisAvailableStock;
    }

    public void setRedisAvailableStock(Integer redisAvailableStock) {
        this.redisAvailableStock = redisAvailableStock;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
