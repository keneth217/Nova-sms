package com.novastack.sms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class SmsBillingCalculator {

    private final BillingSettingsService billingSettingsService;

    public SmsBillingQuote quote(int smsUnits) {
        int units = Math.max(0, smsUnits);
        BigDecimal customerPrice = billingSettingsService.customerPrice();
        BigDecimal providerPerUnit = billingSettingsService.providerCostPerSms();
        String currency = billingSettingsService.currency();
        BigDecimal customerCharge = customerPrice.multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal providerCost = providerPerUnit.multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossMargin = customerCharge.subtract(providerCost).setScale(2, RoundingMode.HALF_UP);
        return new SmsBillingQuote(
                units,
                customerPrice,
                providerPerUnit,
                customerCharge,
                providerCost,
                grossMargin,
                currency
        );
    }

    public long availableSms(BigDecimal walletBalance) {
        BigDecimal price = billingSettingsService.customerPrice();
        if (walletBalance == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return walletBalance.max(BigDecimal.ZERO).divideToIntegralValue(price).longValue();
    }
}
