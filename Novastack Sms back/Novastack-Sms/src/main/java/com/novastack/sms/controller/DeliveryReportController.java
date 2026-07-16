package com.novastack.sms.controller;

import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.service.DeliveryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dlr")
@RequiredArgsConstructor
@Tag(name = "Delivery Reports")
public class DeliveryReportController {

    private final DeliveryReportService deliveryReportService;

    @PostMapping("/callback")
    @Operation(summary = "Africa's Talking delivery report callback")
    public ApiResponse<Void> callback(@RequestParam Map<String, String> payload) {
        deliveryReportService.handleCallback(payload);
        return ApiResponse.ok("DLR received", null);
    }
}
