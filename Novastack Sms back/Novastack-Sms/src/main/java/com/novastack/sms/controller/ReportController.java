package com.novastack.sms.controller;

import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.DashboardReportResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard analytics for the organization")
    public ApiResponse<DashboardReportResponse> dashboard() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(reportService.dashboard(orgId));
    }
}
