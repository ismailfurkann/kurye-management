package com.ivedi.ivedikuryemanagement.dispatch.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionService;
import com.ivedi.ivedikuryemanagement.common.exception.InvalidStatusTransitionException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderService;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import com.ivedi.ivedikuryemanagement.dispatch.api.*;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private CourierService courierService;
    @Mock private DeliveryOrderService deliveryOrderService;
    @Mock private DeliveryAssignmentMapper assignmentMapper;
    @Mock private AssignmentResponseMapper assignmentResponseMapper;
    @Mock private CommissionService commissionService;

    private AssignmentServiceImpl service;
    private UUID userId;
    private UUID courierId;
    private UUID assignmentId;
    private DeliveryAssignment assignment;
    private AssignmentResponse response;

    @BeforeEach
    void setUp() {
        service = new AssignmentServiceImpl(
                assignmentRepository,
                courierRepository,
                courierService,
                deliveryOrderService,
                assignmentMapper,
                assignmentResponseMapper,
                commissionService
        );
        userId = UUID.randomUUID();
        courierId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fullName("Kurye")
                .email("courier@test.com")
                .role(UserRole.COURIER)
                .build();
        Courier courier = Courier.builder().id(courierId).user(user).build();
        Restaurant restaurant = Restaurant.builder().id(UUID.randomUUID()).name("Restoran").owner(user).build();
        Branch branch = Branch.builder().id(UUID.randomUUID()).name("Merkez").restaurant(restaurant).build();
        DeliveryOrder order = DeliveryOrder.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .orderCode("ORD-1")
                .customerName("Ali")
                .customerPhone("+905551234567")
                .customerAddressText("Adres")
                .orderAmount(new BigDecimal("100.00"))
                .deliveryFee(new BigDecimal("10.00"))
                .paymentCollectionType(PaymentCollectionType.CASH_ON_DELIVERY)
                .status(DeliveryOrderStatus.ASSIGNED)
                .createdBy(userId)
                .build();
        assignment = DeliveryAssignment.builder()
                .id(assignmentId)
                .deliveryOrder(order)
                .courier(courier)
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(userId)
                .assignedAt(OffsetDateTime.now())
                .build();
        response = new AssignmentResponse(
                assignmentId,
                order.getId(),
                "ORD-1",
                courierId,
                "Kurye",
                null,
                null,
                AssignmentStatus.ACCEPTED,
                assignment.getAssignedAt(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                "Restoran",
                "Merkez",
                "Adres",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                PaymentCollectionType.CASH_ON_DELIVERY,
                null,
                "Ali",
                "+905551234567"
        );
        setSecurity(user);
        when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(courier));
    }

    @Test
    void acceptResponse_acceptsAssignedAssignmentAndReturnsFrontendResponse() {
        DeliveryAssignmentDto dto = dto(AssignmentStatus.ACCEPTED);
        when(assignmentRepository.findByIdAndCourierId(assignmentId, courierId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);
        when(assignmentMapper.toDto(assignment)).thenReturn(dto);
        when(assignmentRepository.findByIdWithDetails(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentResponseMapper.toResponse(assignment)).thenReturn(response);

        AssignmentResponse result = service.acceptResponse(assignmentId);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(assignment.getAcceptedAt()).isNotNull();
        assertThat(result).isEqualTo(response);
    }

    @Test
    void pickupWithoutAccept_isRejected() {
        when(assignmentRepository.findByIdAndCourierId(assignmentId, courierId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.confirmPickupResponse(assignmentId))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    private DeliveryAssignmentDto dto(AssignmentStatus status) {
        return new DeliveryAssignmentDto(
                assignmentId,
                assignment.getDeliveryOrder().getId(),
                "ORD-1",
                courierId,
                "Kurye",
                status,
                userId,
                assignment.getAssignedAt(),
                assignment.getAcceptedAt(),
                assignment.getPickedUpAt(),
                assignment.getDeliveredAt(),
                assignment.getFailedAt(),
                assignment.getCancelledAt(),
                assignment.getFailureReason(),
                assignment.getCancellationReason()
        );
    }

    private void setSecurity(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
