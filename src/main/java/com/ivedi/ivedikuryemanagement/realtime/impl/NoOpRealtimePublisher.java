package com.ivedi.ivedikuryemanagement.realtime.impl;

import com.ivedi.ivedikuryemanagement.realtime.api.AssignmentEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.CourierLocationEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.OrderStatusEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "realtime.redis.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpRealtimePublisher implements RealtimePublisher {

    @Override
    public void publishOrderStatus(OrderStatusEvent event) {
        log.debug("NoOp: publishOrderStatus orderId={}", event.orderId());
    }

    @Override
    public void publishAssignment(AssignmentEvent event) {
        log.debug("NoOp: publishAssignment assignmentId={}", event.assignmentId());
    }

    @Override
    public void publishCourierLocation(CourierLocationEvent event) {
        log.debug("NoOp: publishCourierLocation courierId={}", event.courierId());
    }
}
