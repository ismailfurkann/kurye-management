package com.ivedi.ivedikuryemanagement.realtime.api;

public interface RealtimePublisher {
    void publishOrderStatus(OrderStatusEvent event);
    void publishAssignment(AssignmentEvent event);
    void publishCourierLocation(CourierLocationEvent event);
}
