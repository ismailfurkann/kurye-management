package com.ivedi.ivedikuryemanagement.report.api;

import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;

import java.math.BigDecimal;
import java.util.UUID;

public record CourierPerformanceResponse(
        UUID courierId,
        String courierName,
        VehicleType vehicleType,
        BigDecimal rating,
        long totalAssignments,
        long deliveredCount,
        long failedCount,
        BigDecimal avgDeliveryMinutes
) {
}
