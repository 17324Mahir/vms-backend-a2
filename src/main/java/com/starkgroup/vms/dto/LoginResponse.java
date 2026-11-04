package com.starkgroup.vms.dto;

/**
 * Successful response returned by POST /api/auth/login.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserSummaryDto user
) {}
