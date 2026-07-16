package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.UserManagementResponse;
import com.ivedi.ivedikuryemanagement.auth.api.UserManagementService;
import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<UserManagementResponse>>> search(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID restaurantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                PageResponse.from(userManagementService.search(role, status, search, restaurantId, page, size))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userManagementService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> create(@Valid @RequestBody ManagedUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", userManagementService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ManagedUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userManagementService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("User status updated", userManagementService.setStatus(id, request)));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','PLATFORM_MANAGER','RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Password reset", userManagementService.resetPassword(id, request)));
    }
}
