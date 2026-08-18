package com.novastack.sms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SmsBillingCalculatorTest {

    @Mock
    private BillingSettingsService billingSettingsService;

    private SmsBillingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SmsBillingCalculator(billingSettingsService);
        lenient().when(billingSettingsService.customerPrice()).thenReturn(new BigDecimal("1.00"));
        lenient().when(billingSettingsService.providerCostPerSms()).thenReturn(new BigDecimal("0.35"));
        lenient().when(billingSettingsService.currency()).thenReturn("KES");
    }

    @Test
    void oneSmsUsesCustomerChargeAndProviderCost() {
        SmsBillingQuote quote = calculator.quote(1);
        assertEquals(0, quote.customerCharge().compareTo(new BigDecimal("1.00")));
        assertEquals(0, quote.providerCost().compareTo(new BigDecimal("0.35")));
        assertEquals(0, quote.grossMargin().compareTo(new BigDecimal("0.65")));
        assertEquals("KES", quote.currency());
    }

    @Test
    void tenSmsUsesSmsUnits() {
        SmsBillingQuote quote = calculator.quote(10);
        assertEquals(0, quote.customerCharge().compareTo(new BigDecimal("10.00")));
        assertEquals(0, quote.providerCost().compareTo(new BigDecimal("3.50")));
        assertEquals(0, quote.grossMargin().compareTo(new BigDecimal("6.50")));
    }

    @Test
    void oneHundredSmsUsesSmsUnits() {
        SmsBillingQuote quote = calculator.quote(100);
        assertEquals(0, quote.customerCharge().compareTo(new BigDecimal("100.00")));
        assertEquals(0, quote.providerCost().compareTo(new BigDecimal("35.00")));
        assertEquals(0, quote.grossMargin().compareTo(new BigDecimal("65.00")));
    }

    @Test
    void bulkOneHundredRecipientsUsesSmsUnits() {
        SmsBillingQuote perRecipient = calculator.quote(1);
        BigDecimal customerCharge = perRecipient.customerCharge().multiply(BigDecimal.valueOf(100));
        BigDecimal providerCost = perRecipient.providerCost().multiply(BigDecimal.valueOf(100));
        BigDecimal margin = customerCharge.subtract(providerCost);
        assertEquals(0, customerCharge.compareTo(new BigDecimal("100.00")));
        assertEquals(0, providerCost.compareTo(new BigDecimal("35.00")));
        assertEquals(0, margin.compareTo(new BigDecimal("65.00")));
    }

    @Test
    void multiPartSmsChargesBySmsUnits() {
        SmsBillingQuote quote = calculator.quote(2);
        assertEquals(2, quote.smsUnits());
        assertEquals(0, quote.customerCharge().compareTo(new BigDecimal("2.00")));
        assertEquals(0, quote.providerCost().compareTo(new BigDecimal("0.70")));
        assertEquals(0, quote.grossMargin().compareTo(new BigDecimal("1.30")));
    }

    @Test
    void availableSmsUsesCustomerPriceOnly() {
        assertEquals(1000, calculator.availableSms(new BigDecimal("1000.00")));
        assertEquals(850, calculator.availableSms(new BigDecimal("850.00")));
        assertEquals(0, calculator.availableSms(new BigDecimal("0.99")));
    }
}
