package com.starkgroup.vms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns standardized client-safe JSON ErrorResponse when authentication fails or is missing.
 * Prevents HTML login redirects for REST APIs.
 */
@Component
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public SecurityAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Full authentication is required to access this resource",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
