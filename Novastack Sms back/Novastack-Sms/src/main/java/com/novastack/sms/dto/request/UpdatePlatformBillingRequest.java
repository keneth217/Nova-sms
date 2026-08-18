package com.novastack.sms.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePlatformBillingRequest {

    private BigDecimal customerSmsPrice;
    private BigDecimal providerCost;
    private String currency;
}
