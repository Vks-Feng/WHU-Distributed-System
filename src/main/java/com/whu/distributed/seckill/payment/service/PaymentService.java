package com.whu.distributed.seckill.payment.service;

import com.whu.distributed.seckill.mq.PaymentMessageProducer;
import com.whu.distributed.seckill.mq.dto.PaymentMessage;
import com.whu.distributed.seckill.order.entity.Order;
import com.whu.distributed.seckill.order.service.OrderService;
import com.whu.distributed.seckill.payment.dto.PaymentRequest;
import com.whu.distributed.seckill.payment.dto.PaymentSubmitResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final OrderService orderService;
    private final PaymentMessageProducer paymentMessageProducer;

    public PaymentService(OrderService orderService, PaymentMessageProducer paymentMessageProducer) {
        this.orderService = orderService;
        this.paymentMessageProducer = paymentMessageProducer;
    }

    public PaymentSubmitResponse submit(PaymentRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw new IllegalArgumentException("orderId is required");
        }

        Order order = orderService.findEntityByOrderId(request.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }

        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return new PaymentSubmitResponse(order.getOrderNo(), "PAID", "order already paid");
        }

        if (!"CREATED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("order status does not support payment, current status=" + order.getStatus());
        }

        boolean success = request.getSuccess() == null || request.getSuccess();
        paymentMessageProducer.send(new PaymentMessage(order.getOrderNo(), success, LocalDateTime.now()));

        return new PaymentSubmitResponse(
                order.getOrderNo(),
                success ? "PAYING" : "PAY_FAILED_PENDING",
                success ? "payment request accepted" : "payment failure request accepted"
        );
    }
}
