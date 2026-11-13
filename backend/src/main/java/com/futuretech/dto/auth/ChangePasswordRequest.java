package com.futuretech.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of PUT /api/me/password. */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) String currentPassword,
        @StrongPassword String newPassword) {
}
