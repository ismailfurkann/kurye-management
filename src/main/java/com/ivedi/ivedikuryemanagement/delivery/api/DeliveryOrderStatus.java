package com.ivedi.ivedikuryemanagement.delivery.api;

public enum DeliveryOrderStatus {
    CREATED,
    READY_FOR_DISPATCH,
    ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED;
    }

    public boolean isEditable() {
        return this == CREATED;
    }

    public boolean isCancellableByRestaurant() {
        return this == CREATED || this == READY_FOR_DISPATCH;
    }
}
