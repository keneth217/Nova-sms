package com.novastack.sms.controller;

import com.novastack.sms.domain.enums.MessageChannel;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * WhatsApp outbound API. Nova remains the source of truth; TalkSasa is called with
 * {@code type=whatsapp} on {@code POST /sms/send}. Scheduling stays on Nova.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp")
public class WhatsAppController {

    private final SmsService smsService;

    @PostMapping("/send")
    @Operation(summary = "Send a single WhatsApp message")
    public ApiResponse<SmsMessageResponse> send(@Valid @RequestBody SendSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("WhatsApp queued", smsService.sendSingle(orgId, request, MessageChannel.WHATSAPP));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send WhatsApp to phone numbers and/or a contact group")
    public ApiResponse<BulkSmsResponse> bulk(@Valid @RequestBody BulkSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("Bulk WhatsApp queued", smsService.sendBulk(orgId, request, MessageChannel.WHATSAPP));
    }

    @PostMapping("/batches/{batchId}/resend-failed")
    @Operation(summary = "Resend only failed WhatsApp recipients from a batch")
    public ApiResponse<BulkSmsResponse> resendFailed(@PathVariable UUID batchId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("Failed WhatsApp resent",
                smsService.resendFailed(orgId, batchId, MessageChannel.WHATSAPP));
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "Get a WhatsApp batch and per-recipient statuses")
    public ApiResponse<BulkSmsResponse> getBatch(@PathVariable UUID batchId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.getBatchForOrganization(orgId, batchId, MessageChannel.WHATSAPP));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend a single failed WhatsApp message")
    public ApiResponse<SmsMessageResponse> resend(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("WhatsApp resent", smsService.resend(orgId, id, MessageChannel.WHATSAPP));
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule WhatsApp for later delivery")
    public ApiResponse<BulkSmsResponse> schedule(@Valid @RequestBody ScheduleSmsRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("WhatsApp scheduled", smsService.schedule(orgId, request, MessageChannel.WHATSAPP));
    }

    @GetMapping("/history")
    @Operation(summary = "WhatsApp send history")
    public ApiResponse<Page<SmsMessageResponse>> history(@PageableDefault(size = 20) Pageable pageable) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.history(orgId, pageable, MessageChannel.WHATSAPP));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Refresh delivery status using the Nova message id")
    public ApiResponse<SmsMessageResponse> status(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.refreshStatus(orgId, id, MessageChannel.WHATSAPP));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a WhatsApp message by Nova id")
    public ApiResponse<SmsMessageResponse> get(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.getForOrganization(orgId, id, MessageChannel.WHATSAPP));
    }
}
