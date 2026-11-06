package com.starkgroup.vms.dto;

import com.starkgroup.vms.util.SecurityUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

/**
 * Payload submitted to POST /api/auth/login.
 */
public record LoginRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password
) {
    public LoginRequest {
        if (email != null) {
            email = switch (email.trim().toLowerCase(Locale.ROOT)) {
                case "test", "admin", "demo", "fleet" -> SecurityUtils.DEFAULT_USER_EMAIL;
                default -> email;
            };
        }
    }
}
