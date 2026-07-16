package com.ivedi.ivedikuryemanagement.delivery.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.common.exception.UnauthorizedException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderMapper;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderSearchDto;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryStatusHistoryMapper;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponseMapper;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignmentRepository;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceImplTest {

    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryStatusHistoryRepository historyRepository;
    @Mock private DeliveryOrderMapper deliveryOrderMapper;
    @Mock private DeliveryStatusHistoryMapper statusHistoryMapper;
    @Mock private BranchRepository branchRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private AssignmentResponseMapper assignmentResponseMapper;
    @Mock private UserRepository userRepository;
    @Mock private RealtimePublisher realtimePublisher;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void restaurantStaff_myOrderList_isDeniedAtServiceLevel() {
        DeliveryOrderServiceImpl service = service();
        setCurrentUser(UserRole.RESTAURANT_STAFF);

        assertThatThrownBy(() -> service.getMyOrderResponses(new DeliveryOrderSearchDto(
                null, null, null, null, null, null, null, null, 0, 20)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("staff branch scope");

        verifyNoInteractions(deliveryOrderRepository);
    }

    @Test
    void restaurantStaff_orderDetail_isDeniedAfterResourceLoadBeforeOwnershipTrust() {
        DeliveryOrderServiceImpl service = service();
        setCurrentUser(UserRole.RESTAURANT_STAFF);
        UUID orderId = UUID.randomUUID();
        Restaurant restaurant = Restaurant.builder().id(UUID.randomUUID()).name("Restaurant").build();
        Branch branch = Branch.builder().id(UUID.randomUUID()).restaurant(restaurant).name("Branch").build();
        DeliveryOrder order = DeliveryOrder.builder().id(orderId).branch(branch).build();
        when(deliveryOrderRepository.findByIdWithBranch(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getResponseById(orderId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("staff branch scope");

        verifyNoInteractions(restaurantRepository);
    }

    private DeliveryOrderServiceImpl service() {
        return new DeliveryOrderServiceImpl(
                deliveryOrderRepository,
                historyRepository,
                deliveryOrderMapper,
                statusHistoryMapper,
                branchRepository,
                restaurantRepository,
                assignmentRepository,
                assignmentResponseMapper,
                userRepository,
                realtimePublisher
        );
    }

    private void setCurrentUser(UserRole role) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("staff@example.com")
                .passwordHash("hash")
                .fullName("Staff")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
