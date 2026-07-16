package com.ivedi.ivedikuryemanagement.dispatch.impl;

import com.ivedi.ivedikuryemanagement.audit.api.AuditService;
import com.ivedi.ivedikuryemanagement.realtime.api.AssignmentEvent;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderService;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrderRepository;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponseMapper;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentMapper;
import com.ivedi.ivedikuryemanagement.dispatch.api.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DispatchServiceImpl implements DispatchService {

    private static final List<AssignmentStatus> ACTIVE_STATUSES =
            List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP);

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final CourierRepository courierRepository;
    private final CourierService courierService;
    private final DeliveryOrderService deliveryOrderService;
    private final DeliveryAssignmentMapper assignmentMapper;
    private final AssignmentResponseMapper assignmentResponseMapper;
    private final AuditService auditService;
    private final RealtimePublisher realtimeEventPublisher;

    @Override
    public DeliveryAssignmentDto assign(UUID orderId, UUID courierId) {
        UserPrincipal actor = currentPrincipal();

        DeliveryOrder order = loadOrder(orderId);
        if (order.getStatus() != DeliveryOrderStatus.READY_FOR_DISPATCH) {
            throw new BusinessException(
                    "Order must be READY_FOR_DISPATCH to assign. Current: " + order.getStatus(),
                    "ORDER_NOT_DISPATCHABLE"
            );
        }

        Courier courier = loadCourier(courierId);
        validateCourierDispatchable(courier);

        if (assignmentRepository.existsByDeliveryOrder_IdAndStatusIn(orderId, ACTIVE_STATUSES)) {
            throw new BusinessException(
                    "Order already has an active assignment",
                    "ASSIGNMENT_CONFLICT"
            );
        }

        // Increment courier count (optimistic lock on Courier.version — 409 on conflict)
        courierService.incrementActiveCount(courierId);

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .deliveryOrder(order)
                .courier(courier)
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(actor.getId())
                .assignedAt(OffsetDateTime.now())
                .build();

        DeliveryAssignment saved = assignmentRepository.save(assignment);

        deliveryOrderService.transitionStatus(
                orderId, DeliveryOrderStatus.ASSIGNED, actor.getId(),
                "Assigned to courier " + courier.getUser().getFullName()
        );

        auditService.record(actor.getId(), "ORDER_ASSIGNED", "DeliveryOrder", orderId,
                "courierId=" + courierId + " assignmentId=" + saved.getId());
        realtimeEventPublisher.publishAssignment(
                AssignmentEvent.created(saved.getId(), orderId, order.getOrderCode(), courierId));
        log.info("Order {} assigned to courier {} by {}", orderId, courierId, actor.getId());
        return assignmentMapper.toDto(saved);
    }

    @Override
    public AssignmentResponse assignResponse(UUID orderId, UUID courierId) {
        DeliveryAssignmentDto dto = assign(orderId, courierId);
        return assignmentRepository.findByIdWithDetails(dto.id())
                .map(assignmentResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", dto.id()));
    }

    @Override
    public DeliveryAssignmentDto reassign(UUID orderId, UUID newCourierId, String reason) {
        UserPrincipal actor = currentPrincipal();

        DeliveryOrder order = loadOrder(orderId);
        // Order stays in ASSIGNED while the courier's assignment goes ASSIGNED→ACCEPTED.
        // Reassign is only possible while order hasn't been picked up yet.
        if (order.getStatus() != DeliveryOrderStatus.ASSIGNED) {
            throw new BusinessException(
                    "Order can only be reassigned in ASSIGNED status. Current: " + order.getStatus(),
                    "ORDER_NOT_REASSIGNABLE"
            );
        }

        DeliveryAssignment active = assignmentRepository
                .findActiveByOrderId(orderId, ACTIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(
                        "No active assignment found for order " + orderId,
                        "ASSIGNMENT_NOT_FOUND"
                ));

        if (active.getCourier().getId().equals(newCourierId)) {
            throw new BusinessException(
                    "Cannot reassign to the same courier",
                    "REASSIGN_SAME_COURIER"
            );
        }

        Courier newCourier = loadCourier(newCourierId);
        validateCourierDispatchable(newCourier);

        // Cancel old assignment, decrement old courier
        active.setStatus(AssignmentStatus.CANCELLED);
        active.setCancelledAt(OffsetDateTime.now());
        active.setCancellationReason(reason != null ? reason : "Reassigned by manager");
        assignmentRepository.save(active);
        courierService.decrementActiveCount(active.getCourier().getId());

        // Assign to new courier
        courierService.incrementActiveCount(newCourierId);

        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
                .deliveryOrder(order)
                .courier(newCourier)
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(actor.getId())
                .assignedAt(OffsetDateTime.now())
                .build();

        DeliveryAssignment saved = assignmentRepository.save(newAssignment);
        // Order stays ASSIGNED — reassignment history is captured in delivery_assignments table.

        auditService.record(actor.getId(), "ORDER_REASSIGNED", "DeliveryOrder", orderId,
                "fromCourier=" + active.getCourier().getId() + " toCourier=" + newCourierId
                        + (reason != null ? " reason=" + reason : ""));
        log.info("Order {} reassigned from courier {} to {} by {}",
                orderId, active.getCourier().getId(), newCourierId, actor.getId());
        return assignmentMapper.toDto(saved);
    }

    @Override
    public AssignmentResponse reassignByAssignmentIdResponse(UUID assignmentId, UUID newCourierId, String reason) {
        DeliveryAssignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        DeliveryAssignmentDto dto = reassign(assignment.getDeliveryOrder().getId(), newCourierId, reason);
        return assignmentRepository.findByIdWithDetails(dto.id())
                .map(assignmentResponseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", dto.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentDto> getAssignmentHistory(UUID orderId) {
        if (!deliveryOrderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("DeliveryOrder", orderId);
        }
        return assignmentMapper.toDtoList(
                assignmentRepository.findAllByOrderIdWithCourier(orderId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAssignmentDto getActiveAssignment(UUID orderId) {
        return assignmentRepository
                .findActiveByOrderId(orderId, ACTIVE_STATUSES)
                .map(assignmentMapper::toDto)
                .orElseThrow(() -> new BusinessException(
                        "No active assignment for order " + orderId,
                        "ASSIGNMENT_NOT_FOUND"
                ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeliveryOrder loadOrder(UUID orderId) {
        return deliveryOrderRepository.findByIdWithBranch(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryOrder", orderId));
    }

    private Courier loadCourier(UUID courierId) {
        return courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", courierId));
    }

    private void validateCourierDispatchable(Courier courier) {
        if (courier.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException(
                    "Courier is not approved. Status: " + courier.getApprovalStatus(),
                    "COURIER_NOT_APPROVED"
            );
        }
        if (courier.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new BusinessException(
                    "Courier is not available. Status: " + courier.getAvailabilityStatus(),
                    "COURIER_NOT_AVAILABLE"
            );
        }
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
