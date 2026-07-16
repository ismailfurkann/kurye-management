package com.ivedi.ivedikuryemanagement.restaurant.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.commission.impl.CommissionRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrderRepository;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantMapper;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantSearchDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantService;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantStatus;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantOwnerRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private static final Sort DEFAULT_SORT = Sort.by("name").ascending();

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final CommissionRepository commissionRepository;

    @Override
    public RestaurantDto create(RestaurantRequest request) {
        User owner = resolveOwner(request.ownerId());

        if (request.taxNumber() != null && restaurantRepository.existsByTaxNumber(request.taxNumber())) {
            throw new DuplicateResourceException("Tax number already registered: " + request.taxNumber());
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.name().trim())
                .legalName(trimOrNull(request.legalName()))
                .taxNumber(trimOrNull(request.taxNumber()))
                .phone(trimOrNull(request.phone()))
                .email(trimOrNull(request.email()))
                .owner(owner)
                .status(RestaurantStatus.PENDING)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created: '{}' [{}] owner=[{}]", saved.getName(), saved.getId(), owner.getEmail());
        return restaurantMapper.toDto(saved);
    }

    @Override
    public RestaurantDto update(UUID id, RestaurantRequest request) {
        Restaurant restaurant = findOrThrow(id);

        if (request.taxNumber() != null
                && restaurantRepository.existsByTaxNumberAndIdNot(request.taxNumber(), id)) {
            throw new DuplicateResourceException("Tax number already in use: " + request.taxNumber());
        }


        restaurant.setName(request.name().trim());
        restaurant.setLegalName(trimOrNull(request.legalName()));
        restaurant.setTaxNumber(trimOrNull(request.taxNumber()));
        restaurant.setPhone(trimOrNull(request.phone()));
        restaurant.setEmail(trimOrNull(request.email()));

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant updated: '{}' [{}]", saved.getName(), saved.getId());
        return restaurantMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getById(UUID id) {
        return restaurantMapper.toDto(findOrThrowWithOwner(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantDto> search(RestaurantSearchDto searchDto) {
        return restaurantRepository
                .findAll(
                    RestaurantSpecification.buildFrom(searchDto),
                    PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT)
                )
                .map(restaurantMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantResponse> searchResponses(RestaurantSearchDto searchDto) {
        Page<Restaurant> page = restaurantRepository.findAll(
                RestaurantSpecification.buildFrom(searchDto),
                PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT)
        );
        Metrics metrics = loadMetrics(page.getContent());
        return page.map(restaurant -> toResponse(restaurant, metrics));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDto> getMyRestaurants() {
        UUID currentUserId = resolveCurrentUserId();
        return restaurantMapper.toDtoList(
                restaurantRepository.findAllByOwner_IdOrderByNameAsc(currentUserId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getMyRestaurantResponses() {
        UUID currentUserId = resolveCurrentUserId();
        List<Restaurant> restaurants = restaurantRepository.findAllByOwner_IdOrderByNameAsc(currentUserId);
        Metrics metrics = loadMetrics(restaurants);
        return restaurants.stream()
                .map(restaurant -> toResponse(restaurant, metrics))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getMyRestaurantById(UUID id) {
        UUID currentUserId = resolveCurrentUserId();
        Restaurant restaurant = restaurantRepository.findByIdAndOwner_Id(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        return restaurantMapper.toDto(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getVisibleResponseById(UUID id) {
        UserPrincipal principal = resolveCurrentPrincipal();
        if (principal.getRole() == UserRole.RESTAURANT_OWNER) {
            Restaurant restaurant = restaurantRepository.findByIdAndOwner_Id(id, principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
            return toResponse(restaurant, loadMetrics(List.of(restaurant)));
        }
        return getResponseById(id);
    }

    @Override
    public RestaurantResponse createResponse(RestaurantRequest request) {
        RestaurantDto created = create(request);
        return getResponseById(created.id());
    }

    @Override
    public RestaurantResponse updateResponse(UUID id, RestaurantRequest request) {
        update(id, request);
        return getResponseById(id);
    }

    @Override
    public RestaurantResponse setStatusResponse(UUID id, RestaurantStatusRequest request) {
        setStatus(id, request);
        return getResponseById(id);
    }

    @Override
    public RestaurantResponse changeOwnerResponse(UUID id, RestaurantOwnerRequest request) {
        changeOwner(id, request);
        return getResponseById(id);
    }

    @Override
    public RestaurantDto setStatus(UUID id, RestaurantStatusRequest request) {
        Restaurant restaurant = findOrThrow(id);
        RestaurantStatus previous = restaurant.getStatus();

        if (previous == request.status()) {
            return restaurantMapper.toDto(restaurant);
        }

        restaurant.setStatus(request.status());
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant [{}] status: {} → {}", id, previous, request.status());
        return restaurantMapper.toDto(saved);
    }

    @Override
    public RestaurantDto changeOwner(UUID id, RestaurantOwnerRequest request) {
        Restaurant restaurant = findOrThrow(id);
        User newOwner = resolveOwner(request.ownerId());

        UUID previousOwnerId = restaurant.getOwner().getId();
        restaurant.setOwner(newOwner);
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant [{}] owner changed: {} → {}", id, previousOwnerId, newOwner.getId());
        return restaurantMapper.toDto(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Restaurant findOrThrow(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    private Restaurant findOrThrowWithOwner(UUID id) {
        return restaurantRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    private RestaurantResponse getResponseById(UUID id) {
        Restaurant restaurant = findOrThrowWithOwner(id);
        return toResponse(restaurant, loadMetrics(List.of(restaurant)));
    }

    private User resolveOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        if (owner.getRole() != UserRole.RESTAURANT_OWNER) {
            throw new BusinessException(
                    "User [" + ownerId + "] must have RESTAURANT_OWNER role, found: " + owner.getRole(),
                    "INVALID_OWNER_ROLE"
            );
        }
        return owner;
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

    private Metrics loadMetrics(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            return new Metrics(Map.of(), Map.of(), Map.of());
        }

        List<UUID> ids = restaurants.stream().map(Restaurant::getId).toList();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        OffsetDateTime from = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        return new Metrics(
                toLongMap(branchRepository.countByRestaurantIds(ids)),
                toLongMap(deliveryOrderRepository.countTodayByRestaurantIds(ids, from, to)),
                toBigDecimalMap(commissionRepository.sumNetAmountByRestaurantIds(ids))
        );
    }

    private RestaurantResponse toResponse(Restaurant restaurant, Metrics metrics) {
        UUID id = restaurant.getId();
        User owner = restaurant.getOwner();
        return new RestaurantResponse(
                id,
                restaurant.getName(),
                restaurant.getEmail(),
                restaurant.getPhone(),
                restaurant.getStatus(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                metrics.branchCounts().getOrDefault(id, 0L),
                0L,
                metrics.todayOrderCounts().getOrDefault(id, 0L),
                metrics.totalCommissions().getOrDefault(id, BigDecimal.ZERO),
                restaurant.getLogoUrl(),
                restaurant.getCreatedAt()
        );
    }

    private Map<UUID, Long> toLongMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<UUID, BigDecimal> toBigDecimalMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            Object value = row[1];
            result.put((UUID) row[0], value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()));
        }
        return result;
    }

    private record Metrics(
            Map<UUID, Long> branchCounts,
            Map<UUID, Long> todayOrderCounts,
            Map<UUID, BigDecimal> totalCommissions
    ) {}

    private static String trimOrNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}
