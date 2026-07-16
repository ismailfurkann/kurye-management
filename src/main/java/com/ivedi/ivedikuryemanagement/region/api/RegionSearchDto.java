package com.ivedi.ivedikuryemanagement.region.api;

public record RegionSearchDto(
        String city,
        String district,
        RegionStatus status,
        int page,
        int size
) {
    public RegionSearchDto {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
