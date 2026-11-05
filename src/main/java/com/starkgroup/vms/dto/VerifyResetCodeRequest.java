package com.starkgroup.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to verify 6-digit email OTP and update password.
 */
public record VerifyResetCodeRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        @Size(max = 160, message = "Email cannot exceed 160 characters.")
        String email,

        @NotBlank(message = "Verification code is required.")
        @Size(min = 4, max = 10, message = "Verification code must be between 4 and 10 characters.")
        String code,

        @NotBlank(message = "New password is required.")
        @Size(min = 6, max = 72, message = "New password must be between 6 and 72 characters.")
        String newPassword
) {
}

