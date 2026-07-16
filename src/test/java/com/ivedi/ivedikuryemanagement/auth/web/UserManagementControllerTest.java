package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.*;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserManagementControllerTest {

    private final UserManagementService userManagementService = mock(UserManagementService.class);
    private final UserManagementController controller = new UserManagementController(userManagementService);

    @Test
    void search_returnsPageResponseAndPassesFilters() {
        UUID restaurantId = UUID.randomUUID();
        UserManagementResponse user = response(UserRole.COURIER);
        when(userManagementService.search(UserRole.COURIER, UserStatus.ACTIVE, "ali", restaurantId, 0, 20))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserManagementResponse> page = controller
                .search(UserRole.COURIER, UserStatus.ACTIVE, "ali", restaurantId, 0, 20)
                .getBody()
                .getData();

        assertThat(page.content()).containsExactly(user);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void createStatusAndReset_delegateToService() {
        UUID id = UUID.randomUUID();
        ManagedUserRequest createRequest = new ManagedUserRequest(
                "Ali", "ali@example.com", null, UserRole.COURIER, null, List.of(), "TempPass123",
                new ManagedCourierRequest(com.ivedi.ivedikuryemanagement.courier.api.VehicleType.MOTORBIKE, "06 ABC 123", null));
        UserStatusRequest statusRequest = new UserStatusRequest(UserStatus.PASSIVE);
        ResetPasswordRequest resetRequest = new ResetPasswordRequest("NewTemp123");
        UserManagementResponse user = response(UserRole.COURIER);
        when(userManagementService.create(createRequest)).thenReturn(user);
        when(userManagementService.setStatus(id, statusRequest)).thenReturn(user);
        when(userManagementService.resetPassword(id, resetRequest)).thenReturn(user);

        assertThat(controller.create(createRequest).getBody().getData()).isEqualTo(user);
        assertThat(controller.setStatus(id, statusRequest).getBody().getData()).isEqualTo(user);
        assertThat(controller.resetPassword(id, resetRequest).getBody().getData()).isEqualTo(user);
    }

    private UserManagementResponse response(UserRole role) {
        return new UserManagementResponse(
                UUID.randomUUID(), "Ali", "ali@example.com", null, role, UserStatus.ACTIVE,
                null, null, List.of(), List.of(), null, true, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
