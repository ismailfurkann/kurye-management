package com.ivedi.ivedikuryemanagement.branch.api;

import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(source = "restaurant.id",       target = "restaurantId")
    @Mapping(source = "restaurant.name",     target = "restaurantName")
    @Mapping(source = "region.id",           target = "regionId")
    @Mapping(source = "region.name",         target = "regionName")
    @Mapping(source = "region.city",         target = "regionCity")
    @Mapping(source = "region.district",     target = "regionDistrict")
    BranchDto toDto(Branch branch);

    List<BranchDto> toDtoList(List<Branch> branches);
}
