package com.ivedi.ivedikuryemanagement.auth.api;

import com.ivedi.ivedikuryemanagement.auth.web.ManagedUserRequest;
import com.ivedi.ivedikuryemanagement.auth.web.ResetPasswordRequest;
import com.ivedi.ivedikuryemanagement.auth.web.UserStatusRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserManagementService {
    Page<UserManagementResponse> search(UserRole role, UserStatus status, String search, UUID restaurantId, int page, int size);
    UserManagementResponse getById(UUID id);
    UserManagementResponse create(ManagedUserRequest request);
    UserManagementResponse update(UUID id, ManagedUserRequest request);
    UserManagementResponse setStatus(UUID id, UserStatusRequest request);
    UserManagementResponse resetPassword(UUID id, ResetPasswordRequest request);
}
