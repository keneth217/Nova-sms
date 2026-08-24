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

    private long requestsToday;
    private long requestsThisWeek;
    private long requestsThisMonth;
    private long successfulToday;
    private long failedToday;
    private long smsApiCallsToday;
    private long mpesaApiCallsToday;
    private long smsSendCallsThisMonth;
    private long smsBulkCallsThisMonth;
    private long mpesaStkCallsThisMonth;
    private long mpesaStatusCallsThisMonth;
    private long c2bVerifyCallsThisMonth;
    private long mpesaStkInitiated;
    private long mpesaStkSuccessful;
    private double successRateThisMonth;
    private Double averageDurationMsThisMonth;
    private long http4xxThisMonth;
    private long http5xxThisMonth;
    private List<RequestDailyPoint> requestDaily;
    private List<EndpointCount> topEndpoints;

    @Data
    @Builder
    public static class DailyPoint {
        private String date;
        private long sent;
        private long delivered;
        private long failed;
    }

    @Data
    @Builder
    public static class RequestDailyPoint {
        private String date;
        private long requests;
        private long success;
        private long failed;
        private long sms;
        private long mpesa;
        private double averageDurationMs;
    }

    @Data
    @Builder
    public static class EndpointCount {
        private String path;
        private long count;
    }
}
