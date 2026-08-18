package com.novastack.sms.controller;

import com.novastack.sms.dto.request.CreateApiClientRequest;
import com.novastack.sms.dto.request.UpdateApiClientRequest;
import com.novastack.sms.dto.response.ApiClientCreatedResponse;
import com.novastack.sms.dto.response.ApiClientResponse;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.ApiClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
@Tag(name = "API Clients")
public class ApiClientController {

    private final ApiClientService apiClientService;

    @GetMapping
    @Operation(summary = "List API clients for this organization")
    public ApiResponse<List<ApiClientResponse>> list() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(apiClientService.listForOrganization(orgId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an API client. The plaintext key is returned only once.")
    public ApiResponse<ApiClientCreatedResponse> create(@Valid @RequestBody CreateApiClientRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        request.setOrganizationId(orgId);
        return ApiResponse.ok("Store this API key now. It will not be shown again.",
                apiClientService.create(orgId, request));
    }

    @GetMapping("/{clientId}")
    public ApiResponse<ApiClientResponse> get(@PathVariable UUID clientId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(apiClientService.get(orgId, clientId));
    }

    @PatchMapping("/{clientId}")
    public ApiResponse<ApiClientResponse> update(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateApiClientRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(apiClientService.update(orgId, clientId, request));
    }

    @PostMapping("/{clientId}/rotate")
    @Operation(summary = "Rotate the API key. The new plaintext key is returned only once.")
    public ApiResponse<ApiClientCreatedResponse> rotate(@PathVariable UUID clientId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("Store this API key now. It will not be shown again.",
                apiClientService.rotate(orgId, clientId));
    }

    @PatchMapping("/{clientId}/enabled")
    public ApiResponse<ApiClientResponse> setEnabled(@PathVariable UUID clientId, @RequestParam boolean enabled) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(apiClientService.setEnabled(orgId, clientId, enabled));
    }

    @PostMapping("/{clientId}/revoke")
    public ApiResponse<ApiClientResponse> revoke(@PathVariable UUID clientId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(apiClientService.revoke(orgId, clientId));
    }
}
