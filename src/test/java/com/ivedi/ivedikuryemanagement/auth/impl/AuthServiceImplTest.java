package com.ivedi.ivedikuryemanagement.auth.impl;

import com.ivedi.ivedikuryemanagement.auth.api.*;
import com.ivedi.ivedikuryemanagement.auth.web.AuthResponse;
import com.ivedi.ivedikuryemanagement.auth.web.ChangePasswordRequest;
import com.ivedi.ivedikuryemanagement.auth.web.LoginRequest;
import com.ivedi.ivedikuryemanagement.auth.web.RegisterRequest;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.security.JwtUtil;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private CourierRepository courierRepository;
    @Mock private UserBranchAssignmentRepository branchAssignmentRepository;

    private AuthServiceImpl authService;
    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtUtil,
                userMapper,
                authenticationManager,
                restaurantRepository,
                courierRepository,
                branchAssignmentRepository
        );
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.COURIER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .tokenVersion(2)
                .build();
        testUserDto = new UserDto(
                testUser.getId(),
                testUser.getFullName(),
                testUser.getEmail(),
                null,
                testUser.getRole(),
                testUser.getStatus(),
                null
        );
    }

    @Test
    void login_validCredentials_returnsTokenAndMustChangePasswordFlag() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("jwt.token.here");
        when(jwtUtil.getExpirationTime()).thenReturn(86400000L);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void login_badCredentials_authenticationManagerThrows() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void register_allRoles_rejected() {
        for (UserRole role : UserRole.values()) {
            RegisterRequest request = new RegisterRequest(
                    "User", role.name().toLowerCase() + "@example.com", null, "password123", role
            );

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Public registration is disabled");
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_success_clearsFlagAndIncrementsTokenVersion() {
        setCurrentUser();
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPassword123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPass", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newHash");

        authService.changePassword(request);

        verify(userRepository).save(argThat(u ->
                u.getPasswordHash().equals("$2a$10$newHash")
                        && !u.isMustChangePassword()
                        && u.getTokenVersion() == 3));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBusinessException() {
        setCurrentUser();
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPassword123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void getCurrentUser_withoutRelations_returnsFrontendShape() {
        setCurrentUser();
        testUser.setRole(UserRole.PLATFORM_MANAGER);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        CurrentUserDto result = authService.getCurrentUser();

        assertThat(result.id()).isEqualTo(testUser.getId());
        assertThat(result.role()).isEqualTo(UserRole.PLATFORM_MANAGER);
        assertThat(result.branchIds()).isNotNull().isEmpty();
    }

    @Test
    void getCurrentUser_forRestaurantStaff_returnsAssignedBranchIds() {
        setCurrentUser();
        UUID branchId = UUID.randomUUID();
        testUser.setRole(UserRole.RESTAURANT_STAFF);
        UserBranchAssignment assignment = UserBranchAssignment.builder()
                .user(testUser)
                .branch(Branch.builder().id(branchId).build())
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(branchAssignmentRepository.findAllByUserIdWithBranch(testUser.getId())).thenReturn(List.of(assignment));

        CurrentUserDto result = authService.getCurrentUser();

        assertThat(result.branchIds()).containsExactly(branchId);
    }

    private void setCurrentUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
