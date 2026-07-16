package com.ivedi.ivedikuryemanagement.delivery.api;

import java.time.LocalDate;
import java.util.UUID;

public record DeliveryOrderSearchDto(
        UUID branchId,
        UUID restaurantId,
        DeliveryOrderStatus status,
        String responseStatus,
        String search,
        OrderPriority priority,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page,
        int size
) {
    public DeliveryOrderSearchDto(
            UUID branchId,
            UUID restaurantId,
            DeliveryOrderStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        this(branchId, restaurantId, status, status != null ? status.name() : null, null, null, dateFrom, dateTo, page, size);
    }

    public DeliveryOrderSearchDto {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
