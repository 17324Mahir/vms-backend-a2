package com.starkgroup.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing in-app direct password reset request.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        @Size(max = 160, message = "Email cannot exceed 160 characters.")
        String email,

        @NotBlank(message = "New password is required.")
        @Size(min = 6, max = 72, message = "New password must be between 6 and 72 characters.")
        String newPassword
) {
}

