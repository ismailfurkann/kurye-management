package com.ivedi.ivedikuryemanagement.region.api;

import com.ivedi.ivedikuryemanagement.region.impl.Region;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RegionMapper {

    RegionDto toDto(Region region);

    List<RegionDto> toDtoList(List<Region> regions);
}
