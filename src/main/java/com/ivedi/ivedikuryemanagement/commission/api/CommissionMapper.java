package com.ivedi.ivedikuryemanagement.commission.api;

import com.ivedi.ivedikuryemanagement.commission.impl.Commission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommissionMapper {

    @Mapping(source = "deliveryAssignment.id",            target = "deliveryAssignmentId")
    @Mapping(source = "deliveryOrder.id",                 target = "deliveryOrderId")
    @Mapping(source = "deliveryOrder.orderCode",          target = "orderCode")
    @Mapping(source = "courier.id",                       target = "courierId")
    @Mapping(source = "courier.user.fullName",            target = "courierFullName")
    CommissionDto toDto(Commission commission);
}
