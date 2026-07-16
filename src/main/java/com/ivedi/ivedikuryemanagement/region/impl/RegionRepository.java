package com.ivedi.ivedikuryemanagement.region.impl;

import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID>, JpaSpecificationExecutor<Region> {

    boolean existsByNameAndCityAndDistrict(String name, String city, String district);

    boolean existsByNameAndCityAndDistrictAndIdNot(String name, String city, String district, UUID id);

    List<Region> findAllByStatusOrderByCityAscNameAsc(RegionStatus status);

    List<Region> findAllByCityIgnoreCaseOrderByNameAsc(String city);
}
