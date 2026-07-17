package com.novastack.sms.controller;

import com.novastack.sms.dto.request.ChangePasswordRequest;
import com.novastack.sms.dto.request.ForgotPasswordRequest;
import com.novastack.sms.dto.request.LoginRequest;
import com.novastack.sms.dto.request.ResetPasswordRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.AuthResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.service.AuthService;
import com.novastack.sms.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Dashboard user login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ApiResponse<UserResponse> profile() {
        return ApiResponse.ok(authService.getCurrentProfile());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok("Password changed successfully", null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.requestReset(request.getEmail());
        return ApiResponse.ok(message, null);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset a password with a one-time token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.ok("Password reset successfully", null);
    }
}
