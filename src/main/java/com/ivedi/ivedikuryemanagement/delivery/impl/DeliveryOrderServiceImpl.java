package com.ivedi.ivedikuryemanagement.delivery.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.InvalidStatusTransitionException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.exception.UnauthorizedException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.delivery.api.*;
import com.ivedi.ivedikuryemanagement.delivery.web.CancelDeliveryOrderRequest;
import com.ivedi.ivedikuryemanagement.delivery.web.CreateDeliveryOrderRequest;
import com.ivedi.ivedikuryemanagement.delivery.web.UpdateDeliveryOrderRequest;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponseMapper;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignment;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignmentRepository;
import com.ivedi.ivedikuryemanagement.realtime.api.OrderStatusEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryStatusHistoryMapper statusHistoryMapper;
    private final BranchRepository branchRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final AssignmentResponseMapper assignmentResponseMapper;
    private final UserRepository userRepository;
    private final RealtimePublisher realtimeEventPublisher;

    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP);

    // ── Restaurant: create ────────────────────────────────────────────────────

    @Override
    public DeliveryOrderDto create(CreateDeliveryOrderRequest request) {
        UserPrincipal actor = currentPrincipal();

        Branch branch = branchRepository.findByIdWithDetails(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.branchId()));

        verifyBranchOwnership(branch, actor);

        if (deliveryOrderRepository.existsByOrderCodeAndBranch_Id(request.orderCode(), branch.getId())) {
            throw new BusinessException(
                    "Order code '" + request.orderCode() + "' already exists for this branch",
                    "DUPLICATE_ORDER_CODE"
            );
        }

        DeliveryOrder order = DeliveryOrder.builder()
                .branch(branch)
                .orderCode(request.orderCode())
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .customerAddressText(request.customerAddressText())
                .customerLatitude(request.customerLatitude())
                .customerLongitude(request.customerLongitude())
                .orderAmount(request.orderAmount())
                .deliveryFee(request.deliveryFee())
                .paymentCollectionType(request.paymentCollectionType())
                .deliveryNote(request.deliveryNote())
                .priority(request.priority() != null ? request.priority() : OrderPriority.NORMAL)
                .status(DeliveryOrderStatus.CREATED)
                .createdBy(actor.getId())
                .build();

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        writeHistory(saved, null, DeliveryOrderStatus.CREATED, actor.getId(), "Order created");
        log.info("Delivery order {} created for branch {}", saved.getId(), branch.getId());
        return deliveryOrderMapper.toDto(saved);
    }

    @Override
    public OrderResponse createResponse(CreateDeliveryOrderRequest request) {
        DeliveryOrderDto dto = create(request);
        return getResponseById(dto.id());
    }

    // ── Restaurant: update ────────────────────────────────────────────────────

    @Override
    public DeliveryOrderDto update(UUID id, UpdateDeliveryOrderRequest request) {
        UserPrincipal actor = currentPrincipal();
        DeliveryOrder order = loadWithBranch(id);

        verifyBranchOwnership(order.getBranch(), actor);

        if (order.getStatus() != DeliveryOrderStatus.CREATED) {
            throw new BusinessException(
                    "Order can only be edited in CREATED status. Current status: " + order.getStatus(),
                    "ORDER_NOT_EDITABLE"
            );
        }

        if (request.customerName() != null)         order.setCustomerName(request.customerName());
        if (request.customerPhone() != null)        order.setCustomerPhone(request.customerPhone());
        if (request.customerAddressText() != null)  order.setCustomerAddressText(request.customerAddressText());
        if (request.customerLatitude() != null)     order.setCustomerLatitude(request.customerLatitude());
        if (request.customerLongitude() != null)    order.setCustomerLongitude(request.customerLongitude());
        if (request.orderAmount() != null)          order.setOrderAmount(request.orderAmount());
        if (request.deliveryFee() != null)          order.setDeliveryFee(request.deliveryFee());
        if (request.paymentCollectionType() != null) order.setPaymentCollectionType(request.paymentCollectionType());
        if (request.deliveryNote() != null)         order.setDeliveryNote(request.deliveryNote());
        if (request.priority() != null)             order.setPriority(request.priority());

        return deliveryOrderMapper.toDto(deliveryOrderRepository.save(order));
    }

    @Override
    public OrderResponse updateResponse(UUID id, UpdateDeliveryOrderRequest request) {
        update(id, request);
        return getResponseById(id);
    }

    // ── Restaurant: read ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderDto getById(UUID id) {
        UserPrincipal actor = currentPrincipal();
        DeliveryOrder order = loadWithBranch(id);

        if (isRestaurantRole(actor)) {
            verifyBranchOwnership(order.getBranch(), actor);
        }

        return deliveryOrderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getResponseById(UUID id) {
        UserPrincipal actor = currentPrincipal();
        DeliveryOrder order = loadWithBranch(id);

        if (isRestaurantRole(actor)) {
            verifyBranchOwnership(order.getBranch(), actor);
        }

        return toDetailedResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryOrderDto> getMyOrders(DeliveryOrderSearchDto searchDto) {
        UserPrincipal actor = currentPrincipal();
        denyRestaurantStaff(actor);

        UUID restaurantId = null;
        if (actor.getRole() == UserRole.RESTAURANT_OWNER) {
            Restaurant restaurant = resolveOwnerRestaurant(actor);
            restaurantId = restaurant.getId();
        }
        // RESTAURANT_STAFF: scoped by branchId in searchDto only (no restaurant-level filter yet)

        Specification<DeliveryOrder> spec = buildSpec(searchDto, restaurantId, null);
        Page<DeliveryOrder> page = deliveryOrderRepository.findAll(
                spec,
                PageRequest.of(searchDto.page(), searchDto.size(), Sort.by("createdAt").descending())
        );
        return page.map(deliveryOrderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrderResponses(DeliveryOrderSearchDto searchDto) {
        UserPrincipal actor = currentPrincipal();
        denyRestaurantStaff(actor);

        UUID restaurantId = null;
        if (actor.getRole() == UserRole.RESTAURANT_OWNER) {
            Restaurant restaurant = resolveOwnerRestaurant(actor);
            restaurantId = restaurant.getId();
        }

        Specification<DeliveryOrder> spec = buildSpec(searchDto, restaurantId, null);
        Page<DeliveryOrder> page = deliveryOrderRepository.findAll(
                spec,
                PageRequest.of(searchDto.page(), searchDto.size(), Sort.by("createdAt").descending())
        );
        return mapOrderPage(page, false);
    }

    // ── Restaurant: submit for dispatch ──────────────────────────────────────

    @Override
    public DeliveryOrderDto submitForDispatch(UUID id) {
        UserPrincipal actor = currentPrincipal();
        DeliveryOrder order = loadWithBranch(id);

        verifyBranchOwnership(order.getBranch(), actor);

        if (order.getStatus() != DeliveryOrderStatus.CREATED) {
            throw new InvalidStatusTransitionException(order.getStatus(), DeliveryOrderStatus.READY_FOR_DISPATCH);
        }

        order.setStatus(DeliveryOrderStatus.READY_FOR_DISPATCH);
        order.setSubmittedAt(OffsetDateTime.now());

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        writeHistory(saved, DeliveryOrderStatus.CREATED, DeliveryOrderStatus.READY_FOR_DISPATCH,
                actor.getId(), "Submitted for dispatch");
        log.info("Order {} submitted for dispatch", id);
        return deliveryOrderMapper.toDto(saved);
    }

    @Override
    public OrderResponse submitForDispatchResponse(UUID id) {
        submitForDispatch(id);
        return getResponseById(id);
    }

    // ── Restaurant/Manager: cancel ────────────────────────────────────────────

    @Override
    public DeliveryOrderDto cancel(UUID id, CancelDeliveryOrderRequest request) {
        UserPrincipal actor = currentPrincipal();
        DeliveryOrder order = loadWithBranch(id);

        DeliveryOrderStatus currentStatus = order.getStatus();

        if (isRestaurantRole(actor)) {
            verifyBranchOwnership(order.getBranch(), actor);
            if (currentStatus != DeliveryOrderStatus.CREATED
                    && currentStatus != DeliveryOrderStatus.READY_FOR_DISPATCH) {
                throw new BusinessException(
                        "Restaurant can only cancel orders in CREATED or READY_FOR_DISPATCH status",
                        "CANCEL_NOT_ALLOWED"
                );
            }
        } else {
            // Manager or Admin
            if (currentStatus == DeliveryOrderStatus.DELIVERED
                    || currentStatus == DeliveryOrderStatus.CANCELLED
                    || currentStatus == DeliveryOrderStatus.FAILED) {
                throw new BusinessException(
                        "Cannot cancel an order in " + currentStatus + " status",
                        "CANCEL_NOT_ALLOWED"
                );
            }
        }

        order.setStatus(DeliveryOrderStatus.CANCELLED);
        order.setCancellationReason(request.reason());
        order.setCancelledAt(OffsetDateTime.now());

        DeliveryOrder saved = deliveryOrderRepository.save(order);
        writeHistory(saved, currentStatus, DeliveryOrderStatus.CANCELLED, actor.getId(), request.reason());
        log.info("Order {} cancelled by {} ({})", id, actor.getId(), actor.getRole());
        return deliveryOrderMapper.toDto(saved);
    }

    @Override
    public OrderResponse cancelResponse(UUID id, CancelDeliveryOrderRequest request) {
        cancel(id, request);
        return getResponseById(id);
    }

    // ── Manager / Admin ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryOrderDto> search(DeliveryOrderSearchDto searchDto) {
        Specification<DeliveryOrder> spec = buildSpec(searchDto, null, null);
        Page<DeliveryOrder> page = deliveryOrderRepository.findAll(
                spec,
                PageRequest.of(searchDto.page(), searchDto.size(), Sort.by("createdAt").descending())
        );
        return page.map(deliveryOrderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchResponses(DeliveryOrderSearchDto searchDto) {
        UserPrincipal actor = currentPrincipal();
        if (isRestaurantRole(actor)) {
            return getMyOrderResponses(searchDto);
        }

        Specification<DeliveryOrder> spec = buildSpec(searchDto, null, null);
        Page<DeliveryOrder> page = deliveryOrderRepository.findAll(
                spec,
                PageRequest.of(searchDto.page(), searchDto.size(), Sort.by("createdAt").descending())
        );
        return mapOrderPage(page, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderDto> getReadyForDispatch() {
        return deliveryOrderMapper.toDtoList(
                deliveryOrderRepository.findReadyForDispatchOrdered()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getReadyResponsesForDispatch() {
        List<DeliveryOrder> orders = deliveryOrderRepository.findReadyForDispatchOrdered();
        Map<UUID, AssignmentResponse> activeAssignments = activeAssignmentsByOrderId(orders);
        return orders.stream()
                .map(order -> toResponse(order, Collections.emptyList(), activeAssignments.get(order.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryOrderDto> getActiveOrders() {
        return deliveryOrderMapper.toDtoList(
                deliveryOrderRepository.findActiveOrders(
                        List.of(DeliveryOrderStatus.ASSIGNED, DeliveryOrderStatus.PICKED_UP)
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryStatusHistoryDto> getStatusHistory(UUID id) {
        if (!deliveryOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("DeliveryOrder", id);
        }
        return statusHistoryMapper.toDtoList(
                historyRepository.findAllByDeliveryOrder_IdOrderByChangedAtAsc(id)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistoryResponses(UUID id) {
        if (!deliveryOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("DeliveryOrder", id);
        }
        return toHistoryResponses(historyRepository.findAllByDeliveryOrder_IdOrderByChangedAtAsc(id));
    }

    // ── Internal — called by dispatch and assignment modules ──────────────────

    @Override
    public void transitionStatus(UUID orderId, DeliveryOrderStatus newStatus, UUID actorUserId, String note) {
        DeliveryOrder order = deliveryOrderRepository.findByIdWithBranch(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryOrder", orderId));

        DeliveryOrderStatus oldStatus = order.getStatus();
        validateTransition(oldStatus, newStatus);

        OffsetDateTime now = OffsetDateTime.now();
        order.setStatus(newStatus);

        switch (newStatus) {
            case ASSIGNED      -> order.setAssignedAt(now);
            case PICKED_UP     -> order.setPickedUpAt(now);
            case DELIVERED     -> order.setDeliveredAt(now);
            case CANCELLED     -> order.setCancelledAt(now);
            case FAILED        -> order.setFailedAt(now);
            default            -> { /* no timestamp for other transitions */ }
        }

        deliveryOrderRepository.save(order);
        writeHistory(order, oldStatus, newStatus, actorUserId, note);
        log.info("Order {} transitioned {} → {} by {}", orderId, oldStatus, newStatus, actorUserId);

        realtimeEventPublisher.publishOrderStatus(OrderStatusEvent.of(
                orderId,
                order.getOrderCode(),
                order.getBranch().getId(),
                order.getBranch().getRestaurant().getId(),
                oldStatus,
                newStatus,
                actorUserId
        ));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private DeliveryOrder loadWithBranch(UUID id) {
        return deliveryOrderRepository.findByIdWithBranch(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryOrder", id));
    }

    private void writeHistory(DeliveryOrder order, DeliveryOrderStatus oldStatus,
                              DeliveryOrderStatus newStatus, UUID actorId, String note) {
        historyRepository.save(DeliveryStatusHistory.builder()
                .deliveryOrder(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedByUserId(actorId)
                .changedAt(OffsetDateTime.now())
                .note(note)
                .build());
    }

    private void verifyBranchOwnership(Branch branch, UserPrincipal actor) {
        denyRestaurantStaff(actor);
        // RESTAURANT_STAFF has role-level trust until a staff↔branch mapping table is introduced.
        // RESTAURANT_OWNER must own the restaurant that the branch belongs to.
        if (actor.getRole() == UserRole.RESTAURANT_OWNER) {
            List<Restaurant> owned = restaurantRepository.findAllByOwner_IdOrderByNameAsc(actor.getId());
            boolean owns = owned.stream()
                    .anyMatch(r -> r.getId().equals(branch.getRestaurant().getId()));
            if (!owns) {
                throw new UnauthorizedException("You do not have access to this branch");
            }
        }
    }

    private Restaurant resolveOwnerRestaurant(UserPrincipal actor) {
        List<Restaurant> restaurants = restaurantRepository.findAllByOwner_IdOrderByNameAsc(actor.getId());
        if (restaurants.isEmpty()) {
            throw new BusinessException("No restaurant found for current user", "RESTAURANT_NOT_FOUND");
        }
        return restaurants.get(0);
    }

    private boolean isRestaurantRole(UserPrincipal actor) {
        return actor.getRole() == UserRole.RESTAURANT_OWNER
                || actor.getRole() == UserRole.RESTAURANT_STAFF;
    }

    private void denyRestaurantStaff(UserPrincipal actor) {
        if (actor.getRole() == UserRole.RESTAURANT_STAFF) {
            throw new UnauthorizedException("Restaurant staff branch scope is not configured");
        }
    }

    private Specification<DeliveryOrder> buildSpec(DeliveryOrderSearchDto dto,
                                                    UUID restaurantId,
                                                    UUID branchId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("branch", JoinType.LEFT).fetch("restaurant", JoinType.LEFT);
                query.distinct(true);
            }

            if (restaurantId != null) {
                predicates.add(cb.equal(
                        root.get("branch").get("restaurant").get("id"), restaurantId));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (dto.branchId() != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), dto.branchId()));
            }
            if (dto.restaurantId() != null && restaurantId == null) {
                predicates.add(cb.equal(
                        root.get("branch").get("restaurant").get("id"), dto.restaurantId()));
            }
            if (dto.responseStatus() != null && "ACCEPTED".equalsIgnoreCase(dto.responseStatus())) {
                Subquery<UUID> acceptedAssignments = query.subquery(UUID.class);
                var assignmentRoot = acceptedAssignments.from(DeliveryAssignment.class);
                acceptedAssignments.select(assignmentRoot.get("deliveryOrder").get("id"));
                acceptedAssignments.where(cb.equal(assignmentRoot.get("status"), AssignmentStatus.ACCEPTED));
                predicates.add(root.get("id").in(acceptedAssignments));
            } else if (dto.status() != null) {
                predicates.add(cb.equal(root.get("status"), dto.status()));
            }
            if (dto.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), dto.priority()));
            }
            if (dto.search() != null && !dto.search().isBlank()) {
                String pattern = "%" + dto.search().trim().toLowerCase() + "%";
                Join<Object, Object> branchJoin = root.join("branch", JoinType.LEFT);
                Join<Object, Object> restaurantJoin = branchJoin.join("restaurant", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), pattern),
                        cb.like(cb.lower(root.get("customerName")), pattern),
                        cb.like(cb.lower(root.get("customerPhone")), pattern),
                        cb.like(cb.lower(root.get("customerAddressText")), pattern),
                        cb.like(cb.lower(branchJoin.get("name")), pattern),
                        cb.like(cb.lower(restaurantJoin.get("name")), pattern)
                ));
            }
            if (dto.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), dto.dateFrom().atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (dto.dateTo() != null) {
                predicates.add(cb.lessThan(
                        root.get("createdAt"), dto.dateTo().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Page<OrderResponse> mapOrderPage(Page<DeliveryOrder> page, boolean includeHistory) {
        Map<UUID, AssignmentResponse> activeAssignments = activeAssignmentsByOrderId(page.getContent());
        return page.map(order -> toResponse(
                order,
                includeHistory ? toHistoryResponses(historyRepository.findAllByDeliveryOrder_IdOrderByChangedAtAsc(order.getId())) : Collections.emptyList(),
                activeAssignments.get(order.getId())
        ));
    }

    private OrderResponse toDetailedResponse(DeliveryOrder order) {
        AssignmentResponse activeAssignment = assignmentRepository
                .findActiveByOrderId(order.getId(), ACTIVE_ASSIGNMENT_STATUSES)
                .map(assignmentResponseMapper::toResponse)
                .orElse(null);
        return toResponse(order, toHistoryResponses(
                historyRepository.findAllByDeliveryOrder_IdOrderByChangedAtAsc(order.getId())), activeAssignment);
    }

    private OrderResponse toResponse(DeliveryOrder order,
                                     List<StatusHistoryResponse> statusHistory,
                                     AssignmentResponse activeAssignment) {
        Branch branch = order.getBranch();
        Restaurant restaurant = branch != null ? branch.getRestaurant() : null;
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal orderAmount = order.getOrderAmount() != null ? order.getOrderAmount() : BigDecimal.ZERO;
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                restaurant != null ? restaurant.getId() : null,
                restaurant != null ? restaurant.getName() : null,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerAddressText(),
                order.getCustomerLatitude(),
                order.getCustomerLongitude(),
                order.getOrderAmount(),
                order.getDeliveryFee(),
                orderAmount.add(deliveryFee),
                order.getPaymentCollectionType(),
                order.getPriority(),
                order.getDeliveryNote(),
                resolveFrontendStatus(order, activeAssignment),
                statusHistory,
                activeAssignment,
                order.getSubmittedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String resolveFrontendStatus(DeliveryOrder order, AssignmentResponse activeAssignment) {
        if (activeAssignment != null && activeAssignment.status() == AssignmentStatus.ACCEPTED) {
            return AssignmentStatus.ACCEPTED.name();
        }
        return order.getStatus().name();
    }

    private Map<UUID, AssignmentResponse> activeAssignmentsByOrderId(List<DeliveryOrder> orders) {
        List<UUID> orderIds = orders.stream().map(DeliveryOrder::getId).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, AssignmentResponse> result = new HashMap<>();
        assignmentRepository.findActiveByOrderIds(orderIds, ACTIVE_ASSIGNMENT_STATUSES)
                .forEach(assignment -> result.putIfAbsent(
                        assignment.getDeliveryOrder().getId(),
                        assignmentResponseMapper.toResponse(assignment)
                ));
        return result;
    }

    private List<StatusHistoryResponse> toHistoryResponses(List<DeliveryStatusHistory> histories) {
        Map<UUID, String> actorNames = actorNames(histories);
        return histories.stream()
                .map(history -> new StatusHistoryResponse(
                        history.getId(),
                        history.getDeliveryOrder() != null ? history.getDeliveryOrder().getId() : null,
                        history.getOldStatus(),
                        history.getNewStatus(),
                        history.getChangedByUserId(),
                        actorNames.get(history.getChangedByUserId()),
                        history.getNote(),
                        history.getChangedAt()
                ))
                .toList();
    }

    private Map<UUID, String> actorNames(List<DeliveryStatusHistory> histories) {
        List<UUID> actorIds = histories.stream()
                .map(DeliveryStatusHistory::getChangedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (actorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private void validateTransition(DeliveryOrderStatus from, DeliveryOrderStatus to) {
        boolean valid = switch (from) {
            case CREATED             -> to == DeliveryOrderStatus.READY_FOR_DISPATCH || to == DeliveryOrderStatus.CANCELLED;
            case READY_FOR_DISPATCH  -> to == DeliveryOrderStatus.ASSIGNED           || to == DeliveryOrderStatus.CANCELLED;
            case ASSIGNED            -> to == DeliveryOrderStatus.PICKED_UP          || to == DeliveryOrderStatus.CANCELLED
                                             || to == DeliveryOrderStatus.FAILED
                                             || to == DeliveryOrderStatus.READY_FOR_DISPATCH; // courier rejection
            case PICKED_UP           -> to == DeliveryOrderStatus.DELIVERED          || to == DeliveryOrderStatus.FAILED;
            case DELIVERED, CANCELLED, FAILED -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
