package com.ivedi.ivedikuryemanagement.courier.api;

import java.util.UUID;

public record CourierSearchDto(
        AvailabilityStatus availabilityStatus,
        ApprovalStatus approvalStatus,
        VehicleType vehicleType,
        UUID regionId,
        String search,
        int page,
        int size
) {
    public CourierSearchDto(
            AvailabilityStatus availabilityStatus,
            ApprovalStatus approvalStatus,
            VehicleType vehicleType,
            int page,
            int size
    ) {
        this(availabilityStatus, approvalStatus, vehicleType, null, null, page, size);
    }

    public CourierSearchDto {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
