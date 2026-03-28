package com.whu.distributed.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.distributed.seckill.mq.dto.SeckillOrderMessage;
import com.whu.distributed.seckill.order.entity.Order;
import com.whu.distributed.seckill.order.service.OrderService;
import com.whu.distributed.seckill.seckill.service.SeckillCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final SeckillCacheService seckillCacheService;

    public OrderMessageConsumer(ObjectMapper objectMapper,
                                OrderService orderService,
                                SeckillCacheService seckillCacheService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.seckillCacheService = seckillCacheService;
    }

    @KafkaListener(topics = "${seckill.topic.order-create}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        SeckillOrderMessage message = readMessage(payload);

        try {
            Order order = orderService.createOrder(message);
            safelyMarkSuccess(order);
            return;
        } catch (Exception ex) {
            Order existing = orderService.findEntityByOrderId(message.getOrderId());
            if (existing == null) {
                existing = orderService.findEntityByUserAndProduct(message.getUserId(), message.getProductId());
            }

            if (existing != null) {
                safelyMarkSuccess(existing);
                log.info("order message already consumed, orderId={}", message.getOrderId());
                return;
            }

            log.error("consume order message failed, orderId={}", message.getOrderId(), ex);
            seckillCacheService.markFailed(message, ex.getMessage());
        }
    }

    private SeckillOrderMessage readMessage(String payload) {
        try {
            return objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (Exception ex) {
            throw new IllegalStateException("parse order message failed", ex);
        }
    }

    private void safelyMarkSuccess(Order order) {
        try {
            seckillCacheService.markSuccess(order);
        } catch (Exception ex) {
            log.warn("update redis order progress failed, orderId={}", order.getOrderNo(), ex);
        }
    }
}
