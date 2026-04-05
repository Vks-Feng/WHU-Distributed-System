package com.whu.distributed.seckill.payment.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.payment.dto.PaymentRequest;
import com.whu.distributed.seckill.payment.dto.PaymentSubmitResponse;
import com.whu.distributed.seckill.payment.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentSubmitResponse> submit(@RequestBody PaymentRequest request) {
        return ApiResponse.ok(paymentService.submit(request));
    }
}
