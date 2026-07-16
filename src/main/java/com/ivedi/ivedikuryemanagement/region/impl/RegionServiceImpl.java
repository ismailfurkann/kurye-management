package com.ivedi.ivedikuryemanagement.region.impl;

import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.region.api.RegionDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionMapper;
import com.ivedi.ivedikuryemanagement.region.api.RegionSearchDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionService;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import com.ivedi.ivedikuryemanagement.region.web.RegionRequest;
import com.ivedi.ivedikuryemanagement.region.web.RegionStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegionServiceImpl implements RegionService {

    private static final Sort DEFAULT_SORT = Sort.by("city").ascending().and(Sort.by("name").ascending());

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    @Override
    public RegionDto create(RegionRequest request) {
        String name = request.name().trim();
        String city = request.city().trim();
        String district = request.district().trim();

        if (regionRepository.existsByNameAndCityAndDistrict(name, city, district)) {
            throw new DuplicateResourceException(
                    "Region already exists: '" + name + "' in " + city + " / " + district
            );
        }

        Region region = Region.builder()
                .name(name)
                .city(city)
                .district(district)
                .description(request.description())
                .status(RegionStatus.ACTIVE)
                .build();

        Region saved = regionRepository.save(region);
        log.info("Region created: '{}' [{}]", saved.getName(), saved.getId());
        return regionMapper.toDto(saved);
    }

    @Override
    public RegionDto update(UUID id, RegionRequest request) {
        Region region = findOrThrow(id);

        String name = request.name().trim();
        String city = request.city().trim();
        String district = request.district().trim();

        if (regionRepository.existsByNameAndCityAndDistrictAndIdNot(name, city, district, id)) {
            throw new DuplicateResourceException(
                    "Region name already in use in this area: '" + name + "' in " + city + " / " + district
            );
        }

        region.setName(name);
        region.setCity(city);
        region.setDistrict(district);
        region.setDescription(request.description());

        Region saved = regionRepository.save(region);
        log.info("Region updated: '{}' [{}]", saved.getName(), saved.getId());
        return regionMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RegionDto getById(UUID id) {
        return regionMapper.toDto(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegionDto> search(RegionSearchDto searchDto) {
        Pageable pageable = PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT);
        return regionRepository
                .findAll(RegionSpecification.buildFrom(searchDto), pageable)
                .map(regionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionDto> getAllActive() {
        return regionMapper.toDtoList(
                regionRepository.findAllByStatusOrderByCityAscNameAsc(RegionStatus.ACTIVE)
        );
    }

    @Override
    public RegionDto setStatus(UUID id, RegionStatusRequest request) {
        Region region = findOrThrow(id);
        RegionStatus previous = region.getStatus();

        if (previous == request.status()) {
            return regionMapper.toDto(region);
        }

        region.setStatus(request.status());
        Region saved = regionRepository.save(region);
        log.info("Region [{}] status: {} → {}", id, previous, request.status());
        return regionMapper.toDto(saved);
    }

    @Override
    public RegionDto toggleStatus(UUID id) {
        Region region = findOrThrow(id);
        RegionStatus next = region.getStatus() == RegionStatus.ACTIVE
                ? RegionStatus.INACTIVE
                : RegionStatus.ACTIVE;
        region.setStatus(next);
        Region saved = regionRepository.save(region);
        log.info("Region [{}] status toggled to {}", id, next);
        return regionMapper.toDto(saved);
    }

    private Region findOrThrow(UUID id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region", id));
    }
}
