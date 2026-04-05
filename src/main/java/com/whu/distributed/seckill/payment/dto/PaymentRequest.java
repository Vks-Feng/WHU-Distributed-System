package com.whu.distributed.seckill.payment.dto;

public class PaymentRequest {

    private String orderId;
    private Boolean success;

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
}
