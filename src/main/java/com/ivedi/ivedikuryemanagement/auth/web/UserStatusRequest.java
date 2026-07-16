package com.ivedi.ivedikuryemanagement.auth.web;

import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull UserStatus status) {
}
