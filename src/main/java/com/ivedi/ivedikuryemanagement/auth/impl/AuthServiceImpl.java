package com.ivedi.ivedikuryemanagement.auth.impl;

import com.ivedi.ivedikuryemanagement.auth.api.AuthService;
import com.ivedi.ivedikuryemanagement.auth.api.CurrentUserDto;
import com.ivedi.ivedikuryemanagement.auth.api.UserDto;
import com.ivedi.ivedikuryemanagement.auth.api.UserMapper;
import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.web.AuthResponse;
import com.ivedi.ivedikuryemanagement.auth.web.ChangePasswordRequest;
import com.ivedi.ivedikuryemanagement.auth.web.LoginRequest;
import com.ivedi.ivedikuryemanagement.auth.web.RegisterRequest;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.JwtUtil;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final RestaurantRepository restaurantRepository;
    private final CourierRepository courierRepository;
    private final UserBranchAssignmentRepository branchAssignmentRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        String token = jwtUtil.generateToken(new UserPrincipal(user));
        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(token, user);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        throw new BusinessException(
                "Public registration is disabled",
                "PUBLIC_REGISTRATION_DISABLED",
                org.springframework.http.HttpStatus.FORBIDDEN
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getProfile() {
        return userMapper.toDto(resolveCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserDto getCurrentUser() {
        User user = resolveCurrentUser();

        UUID restaurantId = null;
        UUID courierId = null;
        List<UUID> branchIds = List.of();

        if (user.getRole() == UserRole.RESTAURANT_OWNER) {
            restaurantId = restaurantRepository.findAllByOwner_IdOrderByNameAsc(user.getId())
                    .stream()
                    .findFirst()
                    .map(restaurant -> restaurant.getId())
                    .orElse(null);
        }

        if (user.getRole() == UserRole.COURIER) {
            courierId = courierRepository.findByUser_Id(user.getId())
                    .map(courier -> courier.getId())
                    .orElse(null);
        }

        if (user.getRole() == UserRole.RESTAURANT_STAFF) {
            branchIds = branchAssignmentRepository.findAllByUserIdWithBranch(user.getId())
                    .stream()
                    .map(assignment -> assignment.getBranch().getId())
                    .toList();
        }

        return new CurrentUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                restaurantId,
                branchIds,
                courierId
        );
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User user = resolveCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    private User resolveCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime() / 1000)
                .mustChangePassword(user.isMustChangePassword())
                .user(userMapper.toDto(user))
                .build();
    }
}
