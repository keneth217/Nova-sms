package com.novastack.sms.controller;

import com.novastack.sms.dto.request.BulkSmsRequest;
import com.novastack.sms.dto.request.ScheduleSmsRequest;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.BulkSmsResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.IdempotencyService;
import com.novastack.sms.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Tag(name = "SMS")
public class SmsController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final SmsService smsService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/send")
    @Operation(
            summary = "Send a single SMS",
            description = "Requires permission SMS_SEND for scoped API clients. Optional Idempotency-Key "
                    + "replays the original message instead of sending a duplicate.")
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false,
            description = "Unique request id. Reuse with the same body to avoid duplicate SMS.")
    public ApiResponse<SmsMessageResponse> send(
            @Valid @RequestBody SendSmsRequest request,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(IDEMPOTENCY_HEADER);
        String hash = IdempotencyService.hashPayload(
                "send",
                request.getRecipient(),
                request.getMessage(),
                request.getSenderId());
        SmsMessageResponse data = idempotencyService.replayOrRun(
                clientId,
                idempotencyKey,
                hash,
                IdempotencyService.TYPE_SMS,
                () -> {
                    SmsMessageResponse sent = smsService.sendSingle(orgId, request);
                    return new IdempotencyService.ReplayResult<>(sent, sent.getId());
                },
                resourceId -> smsService.getForOrganization(orgId, resourceId));
        return ApiResponse.ok("SMS queued", data);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk SMS to phone numbers and/or a contact group",
            description = "Provide recipients (no group), groupId (all contacts in that org group), or both. "
                    + "Requires SMS_BULK. There is no documented maximum recipient count; provider delivery "
                    + "is chunked using the configured SMS_BATCH_SIZE (default 100). Message max 1600 characters.")
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false,
            description = "Unique request id. Reuse with the same body to avoid duplicate batches.")
    public ApiResponse<BulkSmsResponse> bulk(
            @Valid @RequestBody BulkSmsRequest request,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(IDEMPOTENCY_HEADER);
        String hash = IdempotencyService.hashPayload(
                "bulk",
                request.getMessage(),
                request.getSenderId(),
                request.getGroupId() == null ? "" : request.getGroupId().toString(),
                request.getRecipients() == null ? "" : String.join(",", request.getRecipients()));
        BulkSmsResponse data = idempotencyService.replayOrRun(
                clientId,
                idempotencyKey,
                hash,
                IdempotencyService.TYPE_BATCH,
                () -> {
                    BulkSmsResponse sent = smsService.sendBulk(orgId, request);
                    return new IdempotencyService.ReplayResult<>(sent, sent.getBatchId());
                },
                resourceId -> smsService.getBatchForOrganization(orgId, resourceId));
        return ApiResponse.ok("Bulk SMS queued", data);
    }

    @PostMapping("/batches/{batchId}/resend-failed")
    @Operation(summary = "Resend failed recipients from a batch")
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false)
    public ApiResponse<BulkSmsResponse> resendFailed(
            @PathVariable UUID batchId,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(IDEMPOTENCY_HEADER);
        String hash = IdempotencyService.hashPayload("resend-failed", batchId.toString());
        BulkSmsResponse data = idempotencyService.replayOrRun(
                clientId,
                idempotencyKey,
                hash,
                IdempotencyService.TYPE_BATCH,
                () -> {
                    BulkSmsResponse sent = smsService.resendFailed(orgId, batchId);
                    return new IdempotencyService.ReplayResult<>(sent, sent.getBatchId());
                },
                resourceId -> smsService.getBatchForOrganization(orgId, resourceId));
        return ApiResponse.ok("Failed SMS resent", data);
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "Get a bulk SMS batch and per-recipient statuses")
    public ApiResponse<BulkSmsResponse> getBatch(@PathVariable UUID batchId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.getBatchForOrganization(orgId, batchId));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend a failed SMS")
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false)
    public ApiResponse<SmsMessageResponse> resend(
            @PathVariable UUID id,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(IDEMPOTENCY_HEADER);
        String hash = IdempotencyService.hashPayload("resend", id.toString());
        SmsMessageResponse data = idempotencyService.replayOrRun(
                clientId,
                idempotencyKey,
                hash,
                IdempotencyService.TYPE_SMS,
                () -> {
                    SmsMessageResponse sent = smsService.resend(orgId, id);
                    return new IdempotencyService.ReplayResult<>(sent, sent.getId());
                },
                resourceId -> smsService.getForOrganization(orgId, resourceId));
        return ApiResponse.ok("SMS resent", data);
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

    @GetMapping("/{id}/status")
    @Operation(summary = "Refresh delivery status for an SMS using the Nova SMS id")
    public ApiResponse<SmsMessageResponse> status(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.refreshStatus(orgId, id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an SMS by Nova SMS id")
    public ApiResponse<SmsMessageResponse> get(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(smsService.getForOrganization(orgId, id));
    }
}
