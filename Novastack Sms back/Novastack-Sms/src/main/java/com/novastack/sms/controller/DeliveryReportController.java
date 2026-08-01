package com.novastack.sms.controller;

import com.novastack.sms.dto.request.AfricasTalkingDlrCallback;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.service.DeliveryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Africa's Talking SMS notification callbacks.
 * AT posts application/x-www-form-urlencoded — bind with {@code @RequestParam}, not {@code @RequestBody}.
 * Configure URLs in AT dashboard → SMS → SMS Callback URLs.
 */
@RestController
@RequestMapping("/api/v1/dlr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Africa's Talking Callbacks")
public class DeliveryReportController {

    private final DeliveryReportService deliveryReportService;

    @PostMapping(value = "/callback", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Delivery Reports callback (AT → SMS → SMS Callback URLs → Delivery Reports)")
    public ApiResponse<Void> deliveryReport(@RequestParam Map<String, String> payload) {
        log.info("AT DLR raw form params={}", payload);
        AfricasTalkingDlrCallback callback = AfricasTalkingDlrCallback.from(payload);
        deliveryReportService.handleDeliveryReport(callback);
        return ApiResponse.ok("DLR received", null);
    }

    @PostMapping(value = "/incoming", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Incoming Messages callback")
    public ApiResponse<Void> incoming(@RequestParam Map<String, String> payload) {
        log.info("AT incoming raw form params={}", payload);
        deliveryReportService.handleIncomingMessage(payload);
        return ApiResponse.ok("Incoming SMS received", null);
    }

    @PostMapping(value = "/opt-out", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Bulk SMS Opt Out callback")
    public ApiResponse<Void> optOut(@RequestParam Map<String, String> payload) {
        deliveryReportService.handleBulkOptOut(payload);
        return ApiResponse.ok("Opt-out received", null);
    }

    @PostMapping(value = "/subscription", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Subscription Notifications callback")
    public ApiResponse<Void> subscription(@RequestParam Map<String, String> payload) {
        deliveryReportService.handleSubscription(payload);
        return ApiResponse.ok("Subscription update received", null);
    }
}
