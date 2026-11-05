package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.RegisterRequest;
import com.starkgroup.vms.dto.ResetPasswordRequest;
import com.starkgroup.vms.dto.ResetPasswordResponse;

/**
 * Service managing user authentication, registration, password resets, and JWT issuance.
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
