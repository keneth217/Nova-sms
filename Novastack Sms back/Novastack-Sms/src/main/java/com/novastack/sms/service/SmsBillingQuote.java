package com.novastack.sms.service;

import java.math.BigDecimal;

public record SmsBillingQuote(
        int smsUnits,
        BigDecimal customerPrice,
        BigDecimal providerCostPerUnit,
        BigDecimal customerCharge,
        BigDecimal providerCost,
        BigDecimal grossMargin,
        String currency
) {
}
