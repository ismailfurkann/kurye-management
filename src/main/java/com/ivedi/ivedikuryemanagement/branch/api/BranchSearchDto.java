package com.ivedi.ivedikuryemanagement.branch.api;

import java.util.UUID;

public record BranchSearchDto(
        UUID restaurantId,
        UUID regionId,
        String name,
        BranchStatus status,
        int page,
        int size
) {
    public BranchSearchDto {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
