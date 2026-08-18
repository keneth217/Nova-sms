package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ApiClientUsageResponse {

    private ApiClientResponse client;
    private long totalSms;
    private long successfulSms;
    private long failedSms;
    private long smsUnitsUsed;
    private BigDecimal walletBalance;
    private String walletCurrency;
    private Instant lastRequestAt;
    private Instant lastSmsAt;
    private long smsToday;
    private long smsThisMonth;
    private List<DailyPoint> daily;

    @Data
    @Builder
    public static class DailyPoint {
        private String date;
        private long sent;
        private long delivered;
        private long failed;
    }
}
