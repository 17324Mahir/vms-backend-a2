package com.starkgroup.vms.dto;

/**
 * Public minimal summary of authenticated user.
 * Sensitive data such as password hashes or internal IDs are strictly omitted.
 */
public record UserSummaryDto(
    String email
) {}
