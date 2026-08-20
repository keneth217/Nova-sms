package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlatformOverviewResponse {

    private long organizations;
    private long users;
    private long superAdmins;
    private long totalSmsSent;
    private long pendingSenderIds;
    private long pendingTopups;
    private BigDecimal totalOrgWalletBalance;
    private String currency;
    private BigDecimal lowBalanceThreshold;
}
