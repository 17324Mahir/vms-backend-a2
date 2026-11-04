package com.starkgroup.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload submitted to POST /api/auth/login.
 */
public record LoginRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password
) {}
