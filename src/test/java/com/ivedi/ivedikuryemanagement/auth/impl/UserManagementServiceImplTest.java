package com.ivedi.ivedikuryemanagement.auth.impl;

import com.ivedi.ivedikuryemanagement.audit.api.AuditService;
import com.ivedi.ivedikuryemanagement.auth.api.UserManagementResponse;
import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.web.ManagedCourierRequest;
import com.ivedi.ivedikuryemanagement.auth.web.ManagedUserRequest;
import com.ivedi.ivedikuryemanagement.auth.web.UserStatusRequest;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.region.impl.RegionRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private UserBranchAssignmentRepository branchAssignmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCreatesCourier_createsUserAndApprovedOfflineCourier() {
        setCurrent(UserRole.PLATFORM_ADMIN);
        User saved = user(UserRole.COURIER);
        saved.setId(UUID.randomUUID());
        saved.setMustChangePassword(true);
        ManagedUserRequest request = new ManagedUserRequest(
                "Courier", "courier@example.com", "+905551234567", UserRole.COURIER,
                null, List.of(), "TempPass123",
                new ManagedCourierRequest(VehicleType.MOTORBIKE, "06 ABC 123", null));
        when(userRepository.existsByEmail("courier@example.com")).thenReturn(false);
        when(passwordEncoder.encode("TempPass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(branchAssignmentRepository.findAllByUserIdWithBranch(saved.getId())).thenReturn(List.of());
        when(courierRepository.findByUser_Id(saved.getId())).thenReturn(Optional.of(Courier.builder().id(UUID.randomUUID()).user(saved).vehicleType(VehicleType.MOTORBIKE).build()));

        UserManagementResponse response = service().create(request);

        ArgumentCaptor<Courier> courierCaptor = ArgumentCaptor.forClass(Courier.class);
        verify(courierRepository).save(courierCaptor.capture());
        assertThat(courierCaptor.getValue().getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(courierCaptor.getValue().getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        assertThat(response.mustChangePassword()).isTrue();
    }

    @Test
    void staffCreation_withoutBranches_rejected() {
        setCurrent(UserRole.PLATFORM_ADMIN);
        ManagedUserRequest request = new ManagedUserRequest(
                "Staff", "staff@example.com", null, UserRole.RESTAURANT_STAFF,
                null, List.of(), "TempPass123", null);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(passwordEncoder.encode("TempPass123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one branch");
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() {
        User admin = user(UserRole.PLATFORM_ADMIN);
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);
        setCurrent(UserRole.PLATFORM_ADMIN, UUID.randomUUID());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatus(UserRole.PLATFORM_ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> service().setStatus(admin.getId(), new UserStatusRequest(UserStatus.PASSIVE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Last active platform admin");
    }

    private UserManagementServiceImpl service() {
        return new UserManagementServiceImpl(
                userRepository,
                restaurantRepository,
                branchRepository,
                courierRepository,
                regionRepository,
                branchAssignmentRepository,
                passwordEncoder,
                auditService
        );
    }

    private User user(UserRole role) {
        return User.builder()
                .fullName("User")
                .email("user@example.com")
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private void setCurrent(UserRole role) {
        setCurrent(role, UUID.randomUUID());
    }

    private void setCurrent(UserRole role, UUID id) {
        User actor = user(role);
        actor.setId(id);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(actor));
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
