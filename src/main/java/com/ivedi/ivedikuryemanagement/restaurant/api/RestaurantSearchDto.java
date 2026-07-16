package com.ivedi.ivedikuryemanagement.restaurant.api;

public record RestaurantSearchDto(
        String name,
        RestaurantStatus status,
        int page,
        int size
) {
    public RestaurantSearchDto {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}
