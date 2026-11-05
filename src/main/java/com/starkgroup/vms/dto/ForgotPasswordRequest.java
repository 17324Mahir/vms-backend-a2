package com.starkgroup.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to initiate password reset via 6-digit email verification code.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        @Size(max = 160, message = "Email cannot exceed 160 characters.")
        String email
) {
}

