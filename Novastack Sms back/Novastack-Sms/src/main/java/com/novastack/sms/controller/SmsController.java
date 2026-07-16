package com.novastack.sms.controller;

import com.novastack.sms.dto.request.BulkSmsRequest;
import com.novastack.sms.dto.request.ScheduleSmsRequest;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.BulkSmsResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Tag(name = "SMS")
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/send")
    @Operation(summary = "Send a single SMS")
    public ApiResponse<SmsMessageResponse> send(@Valid @RequestBody SendSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("SMS queued", smsService.sendSingle(orgId, request));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk SMS to phone numbers and/or a contact group",
            description = "Provide recipients (no group), groupId (all contacts in that org group), or both.")
    public ApiResponse<BulkSmsResponse> bulk(@Valid @RequestBody BulkSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("Bulk SMS queued", smsService.sendBulk(orgId, request));
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule SMS for later delivery",
            description = "Provide recipients and/or groupId, plus scheduledAt.")
    public ApiResponse<BulkSmsResponse> schedule(@Valid @RequestBody ScheduleSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("SMS scheduled", smsService.schedule(orgId, request));
    }

    @GetMapping("/history")
    @Operation(summary = "SMS send history")
    public ApiResponse<Page<SmsMessageResponse>> history(@PageableDefault(size = 20) Pageable pageable) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.history(orgId, pageable));
    }
}
