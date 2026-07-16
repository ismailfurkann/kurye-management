package com.ivedi.ivedikuryemanagement.courier.api;

import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourierMapper {

    @Mapping(source = "user.id",       target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "user.email",    target = "userEmail")
    @Mapping(source = "user.phone",    target = "userPhone")
    CourierDto toDto(Courier courier);

    List<CourierDto> toDtoList(List<Courier> couriers);
}
