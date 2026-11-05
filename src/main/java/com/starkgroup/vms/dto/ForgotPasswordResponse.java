package com.starkgroup.vms.dto;

/**
 * Response payload confirming dispatch of password reset verification code.
 */
public record ForgotPasswordResponse(
        String message,
        String email,
        boolean emailSent,
        String demoOtp
) {
}

