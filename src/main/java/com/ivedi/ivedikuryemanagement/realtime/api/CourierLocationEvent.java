package com.ivedi.ivedikuryemanagement.realtime.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CourierLocationEvent(
        String type,           // "COURIER_LOCATION_UPDATED"
        UUID courierId,
        String courierFullName,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime occurredAt
) {
    public static CourierLocationEvent of(UUID courierId, String fullName,
                                          BigDecimal lat, BigDecimal lng) {
        return new CourierLocationEvent("COURIER_LOCATION_UPDATED",
                courierId, fullName, lat, lng, OffsetDateTime.now());
    }
}
