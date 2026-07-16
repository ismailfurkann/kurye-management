package com.ivedi.ivedikuryemanagement.auth.api;

import com.ivedi.ivedikuryemanagement.auth.web.AuthResponse;
import com.ivedi.ivedikuryemanagement.auth.web.ChangePasswordRequest;
import com.ivedi.ivedikuryemanagement.auth.web.LoginRequest;
import com.ivedi.ivedikuryemanagement.auth.web.RegisterRequest;

public interface AuthService {

    /**
     * Authenticates a user and returns a JWT token.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registers a new user. Self-registration is restricted to COURIER and RESTAURANT_OWNER roles.
     * Admin and Manager accounts must be created through the admin panel.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Returns the profile of the currently authenticated user.
     */
    UserDto getProfile();

    /**
     * Returns the frontend session shape for the currently authenticated user.
     */
    CurrentUserDto getCurrentUser();

    /**
     * Changes the password of the currently authenticated user.
     * Requires the current password for verification.
     */
    void changePassword(ChangePasswordRequest request);
}
