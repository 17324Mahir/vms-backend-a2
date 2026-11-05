package com.starkgroup.vms.dto;

/**
 * DTO representing password reset confirmation.
 */
public record ResetPasswordResponse(
        String message,
        String email
) {
}

