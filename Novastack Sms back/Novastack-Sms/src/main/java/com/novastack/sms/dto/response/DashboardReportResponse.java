package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardReportResponse {

    private long smsSentToday;
    private long smsSentThisMonth;
    private long deliveredCount;
    private long failedCount;
    private long pendingCount;
    private double deliveryRate;
    private BigDecimal walletBalance;
    private BigDecimal walletUsageToday;
    private BigDecimal walletUsageThisMonth;
    private BigDecimal costToday;
    private BigDecimal costThisMonth;
    private BigDecimal smsPrice;
    private Long availableSms;
    private long smsSent;
    private long totalSmsUnits;
    private BigDecimal totalAmountSpent;
}
