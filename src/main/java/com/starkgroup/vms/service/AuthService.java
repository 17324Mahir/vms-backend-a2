package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.CheckEmailResponse;
import com.starkgroup.vms.dto.ForgotPasswordRequest;
import com.starkgroup.vms.dto.ForgotPasswordResponse;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;
import com.starkgroup.vms.dto.RegisterRequest;
import com.starkgroup.vms.dto.ResetPasswordRequest;
import com.starkgroup.vms.dto.ResetPasswordResponse;
import com.starkgroup.vms.dto.VerifyResetCodeRequest;

/**
 * Service managing user authentication, registration, password resets, and JWT issuance.
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    CheckEmailResponse checkEmail(String email);

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    ResetPasswordResponse verifyResetCode(VerifyResetCodeRequest request);

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
