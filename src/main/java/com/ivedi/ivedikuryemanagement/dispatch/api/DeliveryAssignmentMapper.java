package com.ivedi.ivedikuryemanagement.dispatch.api;

import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeliveryAssignmentMapper {

    @Mapping(source = "deliveryOrder.id",       target = "deliveryOrderId")
    @Mapping(source = "deliveryOrder.orderCode", target = "orderCode")
    @Mapping(source = "courier.id",             target = "courierId")
    @Mapping(source = "courier.user.fullName",  target = "courierFullName")
    DeliveryAssignmentDto toDto(DeliveryAssignment assignment);

    List<DeliveryAssignmentDto> toDtoList(List<DeliveryAssignment> assignments);
}
