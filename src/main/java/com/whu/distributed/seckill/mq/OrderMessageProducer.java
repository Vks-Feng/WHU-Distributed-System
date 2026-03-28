package com.whu.distributed.seckill.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whu.distributed.seckill.mq.dto.SeckillOrderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;
    private final long sendTimeoutSeconds;

    public OrderMessageProducer(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                @Value("${seckill.topic.order-create}") String topicName,
                                @Value("${seckill.kafka-send-timeout-seconds:5}") long sendTimeoutSeconds) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }

    public void send(SeckillOrderMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topicName, String.valueOf(message.getProductId()), payload)
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize order message failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("send order message interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("send order message failed", ex);
        }
    }
}
