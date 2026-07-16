package com.ivedi.ivedikuryemanagement.region.api;

import com.ivedi.ivedikuryemanagement.region.web.RegionRequest;
import com.ivedi.ivedikuryemanagement.region.web.RegionStatusRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface RegionService {

    /**
     * Creates a new region. Name must be unique within the same city+district combination.
     */
    RegionDto create(RegionRequest request);

    /**
     * Updates name, city, district, or description of an existing region.
     * Uniqueness constraint is re-validated on update.
     */
    RegionDto update(UUID id, RegionRequest request);

    /**
     * Returns a single region by id.
     */
    RegionDto getById(UUID id);

    /**
     * Paginated, filtered list of regions.
     * Results are sorted by city asc, then name asc.
     */
    Page<RegionDto> search(RegionSearchDto searchDto);

    /**
     * Returns all ACTIVE regions ordered by city + name.
     * Intended for lightweight dropdown / select lists.
     */
    List<RegionDto> getAllActive();

    /**
     * Activates or deactivates a region.
     * Deactivating a region does not cascade to branches or couriers — handled at assignment level.
     */
    RegionDto setStatus(UUID id, RegionStatusRequest request);

    /**
     * Toggles region status between ACTIVE and INACTIVE.
     */
    RegionDto toggleStatus(UUID id);
}
