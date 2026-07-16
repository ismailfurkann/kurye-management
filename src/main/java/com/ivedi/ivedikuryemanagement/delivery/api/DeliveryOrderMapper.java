package com.ivedi.ivedikuryemanagement.delivery.api;

import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeliveryOrderMapper {

    @Mapping(source = "branch.id",               target = "branchId")
    @Mapping(source = "branch.name",             target = "branchName")
    @Mapping(source = "branch.restaurant.name",  target = "restaurantName")
    @Mapping(source = "createdBy",               target = "createdByUserId")
    DeliveryOrderDto toDto(DeliveryOrder order);

    List<DeliveryOrderDto> toDtoList(List<DeliveryOrder> orders);
}
