package com.whu.distributed.seckill.product.dto;

import java.math.BigDecimal;

public class ProductDetailResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String status;
    private String servedBy;

    public ProductDetailResponse() {
    }

    public ProductDetailResponse(Long id, String name, BigDecimal price, String status, String servedBy) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
        this.servedBy = servedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServedBy() {
        return servedBy;
    }

    public void setServedBy(String servedBy) {
        this.servedBy = servedBy;
    }
}
