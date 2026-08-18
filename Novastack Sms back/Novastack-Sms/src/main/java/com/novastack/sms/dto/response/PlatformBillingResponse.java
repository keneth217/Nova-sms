package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlatformBillingResponse {

    private String provider;
    private String defaultSenderId;
    private BigDecimal customerSmsPrice;
    private BigDecimal providerCost;
    private BigDecimal grossMargin;
    private String currency;
    private long totalSmsSent;
    private long totalSmsUnits;
    private BigDecimal totalCustomerRevenue;
    private BigDecimal totalEstimatedProviderCost;
    private BigDecimal totalGrossMargin;
}
