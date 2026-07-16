package com.ivedi.ivedikuryemanagement.courier.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.realtime.api.CourierLocationEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.CourierDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierMapper;
import com.ivedi.ivedikuryemanagement.courier.api.CourierResponse;
import com.ivedi.ivedikuryemanagement.courier.api.CourierSearchDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.web.CourierApprovalRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierAvailabilityRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierLocationRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierRegistrationRequest;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentMapper;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierServiceImpl implements CourierService {

    private static final Sort DEFAULT_SORT =
            Sort.by("rating").descending().and(Sort.by("activeAssignmentCount").ascending());

    private final CourierRepository courierRepository;
    private final CourierMapper courierMapper;
    private final UserRepository userRepository;
    private final RealtimePublisher realtimeEventPublisher;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentMapper assignmentMapper;

    // ── Courier self ──────────────────────────────────────────────────────────

    @Override
    public CourierDto createProfile(CourierRegistrationRequest request) {
        UUID currentUserId = resolveCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (user.getRole() != UserRole.COURIER) {
            throw new BusinessException(
                    "Only users with COURIER role can create a courier profile",
                    "INVALID_USER_ROLE"
            );
        }

        if (courierRepository.existsByUser_Id(currentUserId)) {
            throw new DuplicateResourceException("Courier profile already exists for this user");
        }

        Courier courier = Courier.builder()
                .user(user)
                .vehicleType(request.vehicleType())
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .approvalStatus(ApprovalStatus.PENDING)
                .activeAssignmentCount(0)
                .rating(BigDecimal.ZERO)
                .build();

        Courier saved = courierRepository.save(courier);
        log.info("Courier profile created: [{}] user=[{}] vehicle=[{}]",
                saved.getId(), user.getEmail(), saved.getVehicleType());
        return courierMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDto getMyProfile() {
        UUID currentUserId = resolveCurrentUserId();
        return courierMapper.toDto(findByUserIdOrThrow(currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public CourierResponse getMyProfileResponse() {
        UUID currentUserId = resolveCurrentUserId();
        Courier courier = findByUserIdOrThrow(currentUserId);
        return toResponse(courier, loadMetrics(List.of(courier.getId())));
    }

    @Override
    public CourierDto updateAvailability(CourierAvailabilityRequest request) {
        UUID currentUserId = resolveCurrentUserId();
        Courier courier = findByUserIdOrThrow(currentUserId);

        if (courier.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(
                    "Cannot change availability — approval status is: " + courier.getApprovalStatus(),
                    "COURIER_NOT_APPROVED"
            );
        }

        if (request.status() == AvailabilityStatus.BUSY) {
            throw new BusinessException(
                    "BUSY status is managed by the system and cannot be set manually",
                    "INVALID_AVAILABILITY_TRANSITION"
            );
        }

        AvailabilityStatus previous = courier.getAvailabilityStatus();
        courier.setAvailabilityStatus(request.status());
        Courier saved = courierRepository.save(courier);
        log.info("Courier [{}] availability: {} → {}", saved.getId(), previous, request.status());
        return courierMapper.toDto(saved);
    }

    @Override
    public CourierDto updateLocation(CourierLocationRequest request) {
        UUID currentUserId = resolveCurrentUserId();
        Courier courier = findByUserIdOrThrow(currentUserId);

        courier.setCurrentLatitude(request.latitude());
        courier.setCurrentLongitude(request.longitude());
        courier.setLastLocationAt(OffsetDateTime.now());

        Courier saved = courierRepository.save(courier);
        realtimeEventPublisher.publishCourierLocation(
                CourierLocationEvent.of(saved.getId(), saved.getUser().getFullName(),
                        request.latitude(), request.longitude()));
        return courierMapper.toDto(saved);
    }

    @Override
    public CourierResponse updateAvailabilityResponse(CourierAvailabilityRequest request) {
        updateAvailability(request);
        UUID currentUserId = resolveCurrentUserId();
        Courier courier = findByUserIdOrThrow(currentUserId);
        return toResponse(courier, loadMetrics(List.of(courier.getId())));
    }

    @Override
    public CourierResponse updateLocationResponse(CourierLocationRequest request) {
        updateLocation(request);
        UUID currentUserId = resolveCurrentUserId();
        Courier courier = findByUserIdOrThrow(currentUserId);
        return toResponse(courier, loadMetrics(List.of(courier.getId())));
    }

    // ── Admin / Manager ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CourierDto getById(UUID id) {
        return courierMapper.toDto(findByIdWithUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CourierResponse getResponseById(UUID id) {
        return toResponse(findByIdWithUser(id), loadMetrics(List.of(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDto> search(CourierSearchDto searchDto) {
        return courierRepository
                .findAll(
                    CourierSpecification.buildFrom(searchDto),
                    PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT)
                )
                .map(courierMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierResponse> searchResponses(CourierSearchDto searchDto) {
        Page<Courier> page = courierRepository
                .findAll(
                        CourierSpecification.buildFrom(searchDto),
                        PageRequest.of(searchDto.page(), searchDto.size(), DEFAULT_SORT)
                );
        Metrics metrics = loadMetrics(page.getContent().stream().map(Courier::getId).toList());
        return page.map(courier -> toResponse(courier, metrics));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDto> getAvailableForDispatch() {
        return courierMapper.toDtoList(
                courierRepository.findAvailableForDispatch(
                        ApprovalStatus.APPROVED,
                        AvailabilityStatus.AVAILABLE
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierResponse> getAvailableResponsesForDispatch() {
        List<Courier> couriers = courierRepository.findAvailableForDispatch(
                ApprovalStatus.APPROVED,
                AvailabilityStatus.AVAILABLE
        );
        Metrics metrics = loadMetrics(couriers.stream().map(Courier::getId).toList());
        return couriers.stream().map(courier -> toResponse(courier, metrics)).toList();
    }

    @Override
    public CourierDto setApprovalStatus(UUID id, CourierApprovalRequest request) {
        Courier courier = findOrThrow(id);
        ApprovalStatus previous = courier.getApprovalStatus();

        if (previous == request.status()) {
            return courierMapper.toDto(courier);
        }

        courier.setApprovalStatus(request.status());
        if (request.status() == ApprovalStatus.APPROVED && courier.getApprovedAt() == null) {
            courier.setApprovedAt(OffsetDateTime.now());
        }

        // Force offline when access is revoked
        if (request.status() == ApprovalStatus.BLOCKED || request.status() == ApprovalStatus.REJECTED) {
            courier.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        }

        Courier saved = courierRepository.save(courier);
        log.info("Courier [{}] approval: {} → {}", id, previous, request.status());
        return courierMapper.toDto(saved);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @Override
    public CourierResponse setApprovalStatusResponse(UUID id, CourierApprovalRequest request) {
        setApprovalStatus(id, request);
        return toResponse(findByIdWithUser(id), loadMetrics(List.of(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryAssignmentDto> getAssignments(UUID courierId, AssignmentStatus status, int page, int size) {
        ensureCanReadAssignments(courierId);
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.min(100, Math.max(1, size));
        return assignmentRepository.findPageByCourierIdAndStatus(
                courierId,
                status,
                PageRequest.of(normalizedPage, normalizedSize, Sort.by("assignedAt").descending())
        ).map(assignmentMapper::toDto);
    }

    @Override
    public void incrementActiveCount(UUID courierId) {
        Courier courier = findOrThrow(courierId);
        courier.setActiveAssignmentCount(courier.getActiveAssignmentCount() + 1);
        courier.setAvailabilityStatus(AvailabilityStatus.BUSY);
        courierRepository.save(courier);
        log.debug("Courier [{}] active count: {} (BUSY)", courierId, courier.getActiveAssignmentCount());
    }

    @Override
    public void decrementActiveCount(UUID courierId) {
        Courier courier = findOrThrow(courierId);
        int newCount = Math.max(0, courier.getActiveAssignmentCount() - 1);
        courier.setActiveAssignmentCount(newCount);

        if (newCount == 0 && courier.getApprovalStatus() == ApprovalStatus.APPROVED) {
            courier.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        courierRepository.save(courier);
        log.debug("Courier [{}] active count: {} status={}", courierId, newCount, courier.getAvailabilityStatus());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Courier findOrThrow(UUID id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", id));
    }

    private Courier findByIdWithUser(UUID id) {
        return courierRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", id));
    }

    private Courier findByUserIdOrThrow(UUID userId) {
        return courierRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Courier profile not found for user: " + userId
                ));
    }

    private CourierResponse toResponse(Courier courier, Metrics metrics) {
        UUID courierId = courier.getId();
        User user = courier.getUser();
        var region = courier.getRegion();
        return new CourierResponse(
                courierId,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                courier.getVehicleType(),
                courier.getPlateNumber(),
                courier.getAvailabilityStatus(),
                courier.getApprovalStatus(),
                courier.getRating(),
                metrics.activeAssignments().getOrDefault(courierId, 0L),
                metrics.totalDeliveries().getOrDefault(courierId, 0L),
                region != null ? region.getId() : null,
                region != null ? region.getName() : null,
                courier.getCurrentLatitude(),
                courier.getCurrentLongitude(),
                courier.getLastLocationAt(),
                courier.getAvatarUrl(),
                courier.getApprovedAt(),
                courier.getCreatedAt()
        );
    }

    private Metrics loadMetrics(List<UUID> courierIds) {
        if (courierIds == null || courierIds.isEmpty()) {
            return Metrics.empty();
        }
        List<UUID> ids = courierIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Metrics.empty();
        }
        Map<UUID, Long> active = countRows(assignmentRepository.countByCourierIdsAndStatusIn(
                ids,
                List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP)
        ));
        Map<UUID, Long> delivered = countRows(assignmentRepository.countByCourierIdsAndStatusIn(
                ids,
                List.of(AssignmentStatus.DELIVERED)
        ));
        return new Metrics(active, delivered);
    }

    private Map<UUID, Long> countRows(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.stream().collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> ((Number) row[1]).longValue()
        ));
    }

    private void ensureCanReadAssignments(UUID courierId) {
        UserPrincipal principal = resolveCurrentPrincipal();
        UserRole role = principal.getRole();
        if (role == UserRole.PLATFORM_ADMIN || role == UserRole.PLATFORM_MANAGER) {
            return;
        }
        if (role == UserRole.COURIER) {
            Courier currentCourier = findByUserIdOrThrow(principal.getId());
            if (currentCourier.getId().equals(courierId)) {
                return;
            }
        }
        throw new BusinessException("Cannot access assignments for another courier", "FORBIDDEN_COURIER_ASSIGNMENTS");
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

    private record Metrics(Map<UUID, Long> activeAssignments, Map<UUID, Long> totalDeliveries) {
        static Metrics empty() {
            return new Metrics(Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
