package com.ivedi.ivedikuryemanagement.realtime.impl;

import com.ivedi.ivedikuryemanagement.common.config.RedisConfig;
import com.ivedi.ivedikuryemanagement.realtime.api.AssignmentEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.CourierLocationEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.OrderStatusEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "realtime.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RealtimeEventPublisher implements RealtimePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publishOrderStatus(OrderStatusEvent event) {
        publish(RedisConfig.CHANNEL_ORDER_STATUS, event);
    }

    @Async
    public void publishAssignment(AssignmentEvent event) {
        publish(RedisConfig.CHANNEL_ASSIGNMENT, event);
    }

    @Async
    public void publishCourierLocation(CourierLocationEvent event) {
        publish(RedisConfig.CHANNEL_COURIER_LOCATION, event);
    }

    private void publish(String channel, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(channel, json);
        } catch (Exception e) {
            log.error("Failed to publish realtime event to channel '{}': {}", channel, e.getMessage());
        }
    }
}
