package com.ivedi.ivedikuryemanagement.realtime.impl;

import tools.jackson.databind.ObjectMapper;
import com.ivedi.ivedikuryemanagement.common.config.RedisConfig;
import com.ivedi.ivedikuryemanagement.realtime.api.AssignmentEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.CourierLocationEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageHandler implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body    = new String(message.getBody());

        try {
            if (channel.equals(RedisConfig.CHANNEL_ORDER_STATUS)) {
                handleOrderStatus(objectMapper.readValue(body, OrderStatusEvent.class));
            } else if (channel.equals(RedisConfig.CHANNEL_ASSIGNMENT)) {
                handleAssignment(objectMapper.readValue(body, AssignmentEvent.class));
            } else if (channel.equals(RedisConfig.CHANNEL_COURIER_LOCATION)) {
                handleCourierLocation(objectMapper.readValue(body, CourierLocationEvent.class));
            }
        } catch (Exception e) {
            log.error("Failed to process Redis message from channel '{}': {}", channel, e.getMessage());
        }
    }

    private void handleOrderStatus(OrderStatusEvent event) {
        // 1. Notify manager dispatch panel — full queue re-sort on the frontend
        messagingTemplate.convertAndSend("/topic/dispatch/queue", event);

        // 2. Notify the specific order's watchers (restaurant UI)
        messagingTemplate.convertAndSend("/topic/orders/" + event.orderId(), event);

        // 3. Notify the restaurant's topic
        messagingTemplate.convertAndSend("/topic/restaurant/" + event.restaurantId() + "/orders", event);

        log.debug("Order {} broadcasted: {} → {}", event.orderId(), event.oldStatus(), event.newStatus());
    }

    private void handleAssignment(AssignmentEvent event) {
        // 1. Notify the specific courier (user-scoped queue — only their session receives this)
        messagingTemplate.convertAndSendToUser(
                event.courierId().toString(),
                "/queue/assignments",
                event
        );

        // 2. Notify manager dispatch panel
        messagingTemplate.convertAndSend("/topic/dispatch/assignments", event);

        log.debug("Assignment {} ({}) sent to courier {}", event.assignmentId(), event.status(), event.courierId());
    }

    private void handleCourierLocation(CourierLocationEvent event) {
        // Manager map — all courier locations
        messagingTemplate.convertAndSend("/topic/couriers/location", event);

        log.debug("Location update for courier {}: ({}, {})",
                event.courierId(), event.latitude(), event.longitude());
    }
}
