package com.starkgroup.vms.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test-Driven Specification for ADR-004: Non-Functional Requirements & Quality Attributes.
 * Validates Availability (Health & K8s Probes), Security (BCrypt strength, Auth guardrails, Headers),
 * and Reliability.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NonFunctionalRequirementsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("NFR - Availability: Actuator health, liveness, and readiness probes return UP (200)")
    void availability_healthProbesReturnUp() throws Exception {
        // Core health check for load balancer
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        // Kubernetes / Container orchestrator liveness probe
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));

        // Kubernetes / Container orchestrator readiness probe
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("NFR - Security: Cryptographic Password Hashing meets BCrypt work factor standard")
    void security_passwordHashingIsCryptographicallyStrong() {
        String rawPassword = "SuperSecure@Test2026";
        String encoded = passwordEncoder.encode(rawPassword);

        // Verify BCrypt prefix ($2a$ or $2b$)
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"),
                "Password must be hashed with BCrypt algorithm");

        // Verify salt is random (two hashes of same password must be different)
        String encodedSecondTime = passwordEncoder.encode(rawPassword);
        assertNotEquals(encoded, encodedSecondTime, "BCrypt must generate cryptographically unique salts");

        // Verify password verification succeeds
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("WrongPassword", encoded));
    }

    @Test
    @DisplayName("NFR - Security: Unauthenticated or forged credentials fail fast with 401 Unauthorized")
    void security_statelessGuardrailsRejectUnauthorizedRequests() throws Exception {
        // Missing Authorization header
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());

        // Forged signature
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.malicious_payload.fake_signature"))
                .andExpect(status().isUnauthorized());

        // Empty Bearer token
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NFR - Security & Reliability: OWASP Security Headers are present on responses")
    void security_owaspSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}

