package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.config.CorsConfig;
import com.starkgroup.vms.config.SecurityConfig;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.UserSummaryDto;
import com.starkgroup.vms.exception.GlobalExceptionHandler;
import com.starkgroup.vms.exception.InvalidCredentialsException;
import com.starkgroup.vms.service.AuthService;
import com.starkgroup.vms.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 OK and cache headers")
    void login_withValidCredentials_returns200AndCacheHeaders() throws Exception {
        LoginRequest request = new LoginRequest("learner@example.test", "ValidPassword@123");
        LoginResponse response = new LoginResponse(
                "mock.signed.jwt.token",
                "Bearer",
                900L,
                new UserSummaryDto("learner@example.test")
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", is("no-cache")))
                .andExpect(jsonPath("$.accessToken", is("mock.signed.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(900)))
                .andExpect(jsonPath("$.user.email", is("learner@example.test")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials returns 401 with generic message")
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("learner@example.test", "WrongPassword@123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Email or password is incorrect."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Email or password is incorrect.")));
    }

    @Test
    @DisplayName("POST /api/auth/login with blank fields returns 400 with field errors")
    void login_withBlankFields_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid email format returns 400 Bad Request")
    void login_withInvalidEmailFormat_returns400() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/auth/register with valid payload returns 201 Created and JWT token")
    void register_withValidPayload_returns201() throws Exception {
        com.starkgroup.vms.dto.RegisterRequest request =
                new com.starkgroup.vms.dto.RegisterRequest("Tony Stark", "tony@starkgroup.test", "IronManSecret@123");
        LoginResponse response = new LoginResponse(
                "mock.registered.token",
                "Bearer",
                900L,
                new UserSummaryDto("tony@starkgroup.test")
        );

        when(authService.register(any(com.starkgroup.vms.dto.RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.accessToken", is("mock.registered.token")))
                .andExpect(jsonPath("$.user.email", is("tony@starkgroup.test")));
    }
}
