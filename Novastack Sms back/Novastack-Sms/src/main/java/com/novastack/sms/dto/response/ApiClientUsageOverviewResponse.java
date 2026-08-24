package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApiClientUsageOverviewResponse {

    private long requestsToday;
    private long requestsThisWeek;
    private long requestsThisMonth;
    private List<ClientCard> clients;
    private List<ClientRank> byClientThisMonth;

    @Data
    @Builder
    public static class ClientCard {
        private UUID id;
        private String name;
        private String organizationName;
        private String status;
        private long requestsToday;
        private long successfulToday;
        private long failedToday;
        private long smsSent;
        private long mpesaRequestsToday;
        private Instant lastRequestAt;
    }

    @Data
    @Builder
    public static class ClientRank {
        private UUID id;
        private String name;
        private String organizationName;
        private long requests;
    }
}
