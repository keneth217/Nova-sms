package com.novastack.sms.controller;

import com.novastack.sms.dto.request.OrganizationRegisterRequest;
import com.novastack.sms.dto.request.OrganizationSettingsRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.OrganizationResponse;
import com.novastack.sms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations")
public class OrganizationController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new organization and admin user")
    public ApiResponse<OrganizationResponse> register(@Valid @RequestBody OrganizationRegisterRequest request) {
        return ApiResponse.ok("Organization registered", authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated organization's details")
    public ApiResponse<OrganizationResponse> currentOrganization() {
        return ApiResponse.ok(authService.getCurrentOrganization());
    }

    @PatchMapping("/me/settings")
    @Operation(summary = "Update organization details and SMS notification settings")
    public ApiResponse<OrganizationResponse> updateSettings(@Valid @RequestBody OrganizationSettingsRequest request) {
        return ApiResponse.ok("Settings saved", authService.updateSettings(request));
    }
}
