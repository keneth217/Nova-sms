package com.novastack.sms.controller;

import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.dto.response.AdminOrganizationResponse;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.service.AdminService;
import com.novastack.sms.service.SenderIdService;
import com.novastack.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin")
public class AdminController {

    private final AdminService adminService;
    private final SmsService smsService;
    private final SenderIdService senderIdService;

    @GetMapping("/organizations")
    @Operation(summary = "List all registered organizations")
    public ApiResponse<Page<AdminOrganizationResponse>> listOrganizations(
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.listOrganizations(status, search, pageable));
    }

    @GetMapping("/organizations/{organizationId}")
    @Operation(summary = "Get organization details")
    public ApiResponse<AdminOrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
        return ApiResponse.ok(adminService.getOrganization(organizationId));
    }

    @PatchMapping("/organizations/{organizationId}/status")
    @Operation(summary = "Update organization status (ACTIVE / SUSPENDED / PENDING)")
    public ApiResponse<AdminOrganizationResponse> updateOrganizationStatus(
            @PathVariable UUID organizationId,
            @RequestParam OrganizationStatus status) {
        return ApiResponse.ok(adminService.updateOrganizationStatus(organizationId, status));
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users/people across organizations")
    public ApiResponse<Page<UserResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.listUsers(role, organizationId, search, pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.ok(adminService.getUser(userId));
    }

    @PatchMapping("/users/{userId}/enabled")
    @Operation(summary = "Enable or disable a user")
    public ApiResponse<UserResponse> setUserEnabled(
            @PathVariable UUID userId,
            @RequestParam boolean enabled) {
        return ApiResponse.ok(adminService.setUserEnabled(userId, enabled));
    }

    @GetMapping("/overview")
    @Operation(summary = "Platform overview counts")
    public ApiResponse<Map<String, Long>> overview() {
        return ApiResponse.ok(adminService.platformOverview());
    }

    @GetMapping("/topups")
    @Operation(summary = "List wallet top-ups across all organizations")
    public ApiResponse<Page<WalletTransactionResponse>> listTopups(
            @RequestParam(required = false) TopupStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(adminService.listTopups(status, pageable));
    }

    @GetMapping("/sms")
    @Operation(summary = "List SMS messages across all organizations")
    public ApiResponse<Page<SmsMessageResponse>> listSms(@PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(smsService.platformHistory(pageable));
    }

    @GetMapping("/sender-ids")
    @Operation(summary = "List sender IDs across all organizations")
    public ApiResponse<List<SenderId>> listSenderIds(
            @RequestParam(required = false) SenderIdStatus status) {
        return ApiResponse.ok(senderIdService.listAll(status));
    }
}
