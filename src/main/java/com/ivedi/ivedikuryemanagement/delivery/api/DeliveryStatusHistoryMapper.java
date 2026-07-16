package com.ivedi.ivedikuryemanagement.delivery.api;

import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeliveryStatusHistoryMapper {

    @Mapping(source = "deliveryOrder.id", target = "deliveryOrderId")
    DeliveryStatusHistoryDto toDto(DeliveryStatusHistory history);

    List<DeliveryStatusHistoryDto> toDtoList(List<DeliveryStatusHistory> histories);
}
