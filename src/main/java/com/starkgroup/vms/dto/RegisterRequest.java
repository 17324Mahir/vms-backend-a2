package com.starkgroup.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing user registration payload.
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 120, message = "Name cannot exceed 120 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email format.")
        @Size(max = 160, message = "Email cannot exceed 160 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters.")
        String password
) {
}

