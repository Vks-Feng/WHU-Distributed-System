package com.whu.distributed.seckill.mq.dto;

import java.time.LocalDateTime;

public class PaymentMessage {

    private String orderId;
    private Boolean success;
    private LocalDateTime requestedAt;

    public PaymentMessage() {
    }

    public PaymentMessage(String orderId, Boolean success, LocalDateTime requestedAt) {
        this.orderId = orderId;
        this.success = success;
        this.requestedAt = requestedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
