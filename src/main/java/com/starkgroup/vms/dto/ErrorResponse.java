package com.starkgroup.vms.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorDto> fieldErrors
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, List.of());
    }

    public static ErrorResponse of(int status, String code, String message, String path, List<FieldErrorDto> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors != null ? fieldErrors : List.of());
    }
}
