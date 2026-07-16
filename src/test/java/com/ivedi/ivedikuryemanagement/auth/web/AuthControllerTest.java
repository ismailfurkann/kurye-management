package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.AuthService;
import com.ivedi.ivedikuryemanagement.auth.api.CurrentUserDto;
import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.web.RegisterRequest;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    @DisplayName("GET /auth/me returns CurrentUserDto wrapper")
    void getProfile_returnsCurrentUserDto() {
        UUID userId = UUID.randomUUID();
        CurrentUserDto currentUser = new CurrentUserDto(
                userId,
                "Test User",
                "test@example.com",
                null,
                UserRole.PLATFORM_MANAGER,
                null,
                List.of(),
                null
        );
        when(authService.getCurrentUser()).thenReturn(currentUser);

        ResponseEntity<ApiResponse<CurrentUserDto>> response = authController.getProfile();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().id()).isEqualTo(userId);
        assertThat(response.getBody().getData().branchIds()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("POST /auth/logout returns successful no-op wrapper")
    void logout_returnsSuccessWrapper() {
        ResponseEntity<ApiResponse<Void>> response = authController.logout();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Logout successful");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("POST /auth/register returns disabled wrapper")
    void register_returnsDisabledWrapper() {
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.register(new RegisterRequest(
                "Courier", "courier@example.com", null, "Password123", UserRole.COURIER));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("PUBLIC_REGISTRATION_DISABLED");
    }
}
