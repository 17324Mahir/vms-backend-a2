package com.starkgroup.vms.dto;

/**
 * Response payload confirming whether an email address already has an account.
 */
public record CheckEmailResponse(
        String email,
        boolean exists,
        String message
) {
}

