package com.ivedi.ivedikuryemanagement.restaurant.api;

import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {

    @Mapping(source = "owner.id",       target = "ownerId")
    @Mapping(source = "owner.fullName", target = "ownerFullName")
    @Mapping(source = "owner.email",    target = "ownerEmail")
    RestaurantDto toDto(Restaurant restaurant);

    List<RestaurantDto> toDtoList(List<Restaurant> restaurants);
}
