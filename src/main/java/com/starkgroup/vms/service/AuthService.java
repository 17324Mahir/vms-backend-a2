package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.RegisterRequest;

/**
 * Service managing user authentication, registration, and JWT issuance.
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);
}
