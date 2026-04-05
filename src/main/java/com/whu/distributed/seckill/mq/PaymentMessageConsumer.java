package com.whu.distributed.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.distributed.seckill.mq.dto.PaymentMessage;
import com.whu.distributed.seckill.order.entity.Order;
import com.whu.distributed.seckill.order.service.OrderService;
import com.whu.distributed.seckill.seckill.service.SeckillCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentMessageConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final SeckillCacheService seckillCacheService;

    public PaymentMessageConsumer(ObjectMapper objectMapper,
                                  OrderService orderService,
                                  SeckillCacheService seckillCacheService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.seckillCacheService = seckillCacheService;
    }

    @KafkaListener(topics = "${seckill.topic.payment}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        PaymentMessage message = readMessage(payload);

        try {
            if (Boolean.TRUE.equals(message.getSuccess())) {
                Order order = orderService.markPaid(message);
                seckillCacheService.markPaid(order);
                return;
            }

            Order order = orderService.markPaymentFailed(message);
            seckillCacheService.markOrderStatus(order, "payment failed, stock released");
            seckillCacheService.clearUserOrderMarker(order.getUserId(), order.getProductId(), order.getOrderNo());
        } catch (Exception ex) {
            log.error("consume payment message failed, orderId={}", message.getOrderId(), ex);
        }
    }

    private PaymentMessage readMessage(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentMessage.class);
        } catch (Exception ex) {
            throw new IllegalStateException("parse payment message failed", ex);
        }
    }
}
