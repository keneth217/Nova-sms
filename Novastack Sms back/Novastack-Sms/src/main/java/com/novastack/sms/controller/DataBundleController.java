package com.novastack.sms.controller;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.enums.BundleStatus;
import com.novastack.sms.dto.request.DataBundleOffersRequest;
import com.novastack.sms.dto.request.DataBundlePurchaseRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.DataBundleMetricsResponse;
import com.novastack.sms.dto.response.DataBundleOffersResponse;
import com.novastack.sms.dto.response.DataBundleTransactionResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.CallbackService;
import com.novastack.sms.service.MobileDataBundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/data-bundles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Safaricom Data Bundles")
public class DataBundleController {

    private final MobileDataBundleService mobileDataBundleService;
    private final CallbackService callbackService;
    private final AppProperties appProperties;

    @PostMapping("/offers")
    @Operation(summary = "Validate subscriber and fetch Safaricom data bundle offers (public)")
    public ApiResponse<DataBundleOffersResponse> offers(@Valid @RequestBody DataBundleOffersRequest request) {
        return ApiResponse.ok(mobileDataBundleService.fetchOffers(resolveOrganizationId(), request, isPublicCaller()));
    }

    @PostMapping("/purchase")
    @Operation(summary = "Purchase a selected Safaricom data bundle (public)")
    public ApiResponse<DataBundleTransactionResponse> purchase(@Valid @RequestBody DataBundlePurchaseRequest request) {
        return ApiResponse.ok(mobileDataBundleService.purchase(resolveOrganizationId(), request, isPublicCaller()));
    }

    @GetMapping("/status/{reference}")
    @Operation(summary = "Query data-bundle transaction status (public)")
    public ApiResponse<DataBundleTransactionResponse> status(@PathVariable String reference) {
        return ApiResponse.ok(mobileDataBundleService.status(resolveOrganizationId(), reference, isPublicCaller()));
    }

    @GetMapping("/history")
    @Operation(summary = "List organization data-bundle purchase history (authenticated)")
    public ApiResponse<Page<DataBundleTransactionResponse>> history(
            @RequestParam(required = false) BundleStatus status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        if (isPublicCaller()) {
            throw new ApiException("Sign in to view purchase history", HttpStatus.UNAUTHORIZED);
        }
        return ApiResponse.ok(mobileDataBundleService.history(
                SecurityUtils.requireOrganizationId(), status, phone, from, to, pageable));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Data-bundle dashboard metrics (authenticated)")
    public ApiResponse<DataBundleMetricsResponse> metrics() {
        if (isPublicCaller()) {
            throw new ApiException("Sign in to view metrics", HttpStatus.UNAUTHORIZED);
        }
        return ApiResponse.ok(mobileDataBundleService.metrics(SecurityUtils.requireOrganizationId()));
    }

    @PostMapping(value = "/callback", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Safaricom asynchronous data-bundle callback")
    public Map<String, Object> callback(
            @RequestBody(required = false) String body,
            @RequestHeader(value = "X-Callback-Token", required = false) String callbackToken) {
        log.info("Received Safaricom data-bundle callback");
        return callbackService.handleCallback(body, callbackToken);
    }

    private boolean isPublicCaller() {
        return SecurityUtils.optionalOrganizationId().isEmpty();
    }

    private UUID resolveOrganizationId() {
        return SecurityUtils.optionalOrganizationId()
                .orElseGet(this::publicOrganizationId);
    }

    private UUID publicOrganizationId() {
        try {
            return UUID.fromString(appProperties.getDataBundles().getPublicOrganizationId());
        } catch (Exception ex) {
            throw new ApiException("Public data-bundles organization is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
