package com.novastack.sms.controller;

import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.dto.request.SenderIdRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.SenderIdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sender-ids")
@RequiredArgsConstructor
@Tag(name = "Sender IDs")
public class SenderIdController {

    private final SenderIdService senderIdService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request organization sender ID")
    public ApiResponse<SenderId> request(@Valid @RequestBody SenderIdRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(senderIdService.requestSenderId(orgId, request));
    }

    @GetMapping
    @Operation(summary = "List organization sender IDs")
    public ApiResponse<List<SenderId>> list() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(senderIdService.listForOrganization(orgId));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve or reject sender ID (SUPER_ADMIN)")
    public ApiResponse<SenderId> review(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        SenderIdStatus status = SenderIdStatus.valueOf(body.getOrDefault("status", "APPROVED"));
        return ApiResponse.ok(senderIdService.review(id, status, body.get("reason")));
    }
}
