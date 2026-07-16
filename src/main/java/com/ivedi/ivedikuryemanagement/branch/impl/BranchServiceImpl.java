package com.ivedi.ivedikuryemanagement.branch.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.branch.api.BranchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchMapper;
import com.ivedi.ivedikuryemanagement.branch.api.BranchResponse;
import com.ivedi.ivedikuryemanagement.branch.api.BranchSearchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchService;
import com.ivedi.ivedikuryemanagement.branch.api.BranchStatus;
import com.ivedi.ivedikuryemanagement.branch.web.BranchRequest;
import com.ivedi.ivedikuryemanagement.branch.web.BranchStatusRequest;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import com.ivedi.ivedikuryemanagement.region.impl.Region;
import com.ivedi.ivedikuryemanagement.region.impl.RegionRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BranchServiceImpl implements BranchService {

    private static final Sort DEFAULT_SORT = Sort.by("name").ascending();

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final RestaurantRepository restaurantRepository;
    private final RegionRepository regionRepository;

    @Override
    public BranchDto create(BranchRequest request) {
        Restaurant restaurant = findRestaurantOrThrow(request.restaurantId());
        Region region = findActiveRegionOrThrow(request.regionId());

        String name = request.name().trim();
        if (branchRepository.existsByNameAndRestaurant_Id(name, request.restaurantId())) {
            throw new DuplicateResourceException(
                    "Branch '" + name + "' already exists in restaurant: " + restaurant.getName()
            );
        }

        Branch branch = Branch.builder()
                .restaurant(restaurant)
                .region(region)
                .name(name)
                .phone(trimOrNull(request.phone()))
                .addressText(trimOrNull(request.addressText()))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(BranchStatus.ACTIVE)
                .build();

        Branch saved = branchRepository.save(branch);
        log.info("Branch created: '{}' [{}] restaurant=[{}] region=[{}]",
                saved.getName(), saved.getId(), restaurant.getName(), region.getName());
        return branchMapper.toDto(saved);
    }

    @Override
    public BranchDto update(UUID id, BranchRequest request) {
        Branch branch = findOrThrow(id);
        Region region = findActiveRegionOrThrow(request.regionId());

        String name = request.name().trim();
        UUID restaurantId = branch.getRestaurant().getId();

        if (branchRepository.existsByNameAndRestaurant_IdAndIdNot(name, restaurantId, id)) {
            throw new DuplicateResourceException(
                    "Branch '" + name + "' already exists in this restaurant"
            );
        }

        // restaurantId from request is intentionally ignored — restaurant is immutable after creation
        branch.setRegion(region);
        branch.setName(name);
        branch.setPhone(trimOrNull(request.phone()));
        branch.setAddressText(trimOrNull(request.addressText()));
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());

        Branch saved = branchRepository.save(branch);
        log.info("Branch updated: '{}' [{}]", saved.getName(), saved.getId());
        return branchMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDto getById(UUID id) {
        return branchMapper.toDto(findOrThrowWithDetails(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchDto> search(BranchSearchDto searchDto) {
        return branchRepository
                .findAll(
                    BranchSpecification.buildFrom(searchDto),
                    PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT)
                )
                .map(branchMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponse> searchResponses(BranchSearchDto searchDto) {
        return search(searchDto).map(BranchResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> getByRestaurantId(UUID restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        return branchMapper.toDtoList(
                branchRepository.findAllByRestaurant_IdOrderByNameAsc(restaurantId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getByRestaurantIdResponses(UUID restaurantId) {
        return getByRestaurantId(restaurantId).stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getByRestaurantIdResponsesForCurrentUser(UUID restaurantId) {
        UserPrincipal principal = resolveCurrentPrincipal();
        if (principal.getRole() == UserRole.RESTAURANT_OWNER) {
            return getMyBranches(restaurantId).stream()
                    .map(BranchResponse::from)
                    .toList();
        }
        return getByRestaurantIdResponses(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> getMyBranches(UUID restaurantId) {
        verifyRestaurantOwnership(restaurantId);
        return branchMapper.toDtoList(
                branchRepository.findAllByRestaurant_IdOrderByNameAsc(restaurantId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getMyBranchResponses() {
        UserPrincipal principal = resolveCurrentPrincipal();
        if (principal.getRole() == UserRole.RESTAURANT_STAFF) {
            return List.of();
        }
        return branchMapper.toDtoList(branchRepository.findAllByRestaurantOwnerIdWithDetails(principal.getId()))
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDto getMyBranchById(UUID restaurantId, UUID branchId) {
        verifyRestaurantOwnership(restaurantId);
        Branch branch = branchRepository.findByIdAndRestaurant_Id(branchId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        return branchMapper.toDto(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getResponseById(UUID id) {
        return BranchResponse.from(getById(id));
    }

    @Override
    public BranchResponse createResponse(BranchRequest request) {
        return BranchResponse.from(create(request));
    }

    @Override
    public BranchResponse updateResponse(UUID id, BranchRequest request) {
        return BranchResponse.from(update(id, request));
    }

    @Override
    public BranchResponse toggleStatus(UUID id) {
        Branch branch = findOrThrow(id);
        BranchStatus next = branch.getStatus() == BranchStatus.ACTIVE ? BranchStatus.INACTIVE : BranchStatus.ACTIVE;
        return BranchResponse.from(setStatus(id, new BranchStatusRequest(next)));
    }

    @Override
    public BranchDto setStatus(UUID id, BranchStatusRequest request) {
        Branch branch = findOrThrow(id);
        BranchStatus previous = branch.getStatus();

        if (previous == request.status()) {
            return branchMapper.toDto(branch);
        }

        branch.setStatus(request.status());
        Branch saved = branchRepository.save(branch);
        log.info("Branch [{}] status: {} → {}", id, previous, request.status());
        return branchMapper.toDto(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Branch findOrThrow(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private Branch findOrThrowWithDetails(UUID id) {
        return branchRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private Restaurant findRestaurantOrThrow(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
    }

    private Region findActiveRegionOrThrow(UUID regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));
        if (region.getStatus() != RegionStatus.ACTIVE) {
            throw new BusinessException(
                    "Region is not active: " + region.getName() + " [" + regionId + "]",
                    "REGION_NOT_ACTIVE"
            );
        }
        return region;
    }

    private void verifyRestaurantOwnership(UUID restaurantId) {
        UUID currentUserId = resolveCurrentUserId();
        restaurantRepository.findByIdAndOwner_Id(restaurantId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
    }

    private UUID resolveCurrentUserId() {
        return resolveCurrentPrincipal().getId();
    }

    private UserPrincipal resolveCurrentPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up;
        }
        throw new BusinessException("Cannot resolve current user from security context");
    }

    private static String trimOrNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}
