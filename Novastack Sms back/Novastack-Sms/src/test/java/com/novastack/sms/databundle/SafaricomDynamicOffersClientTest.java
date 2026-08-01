package com.novastack.sms.databundle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafaricomDynamicOffersClientTest {

    @Test
    void categorizesDailyWeeklyMonthlyAndPromo() {
        assertEquals("DAILY", SafaricomDynamicOffersClient.categorize("Daily 100MB", "1 Day", null));
        assertEquals("WEEKLY", SafaricomDynamicOffersClient.categorize("Weekly 1GB", "7 Days", null));
        assertEquals("MONTHLY", SafaricomDynamicOffersClient.categorize("Monthly 5GB", "30 Days", null));
        assertEquals("PROMOTIONAL", SafaricomDynamicOffersClient.categorize("Flash deal", "Tonight", "promo"));
        assertEquals("PROMOTIONAL", SafaricomDynamicOffersClient.categorize("CVM offer", "7 Days", "CVM1"));
        assertEquals("OTHER", SafaricomDynamicOffersClient.categorize("Special", null, null));
    }

    @Test
    void convertsPurchaseMsisdnToInternationalFormat() {
        assertEquals("254708374149", SafaricomDynamicOffersClient.toPurchaseMsisdn("254708374149"));
        assertEquals("254795898572", SafaricomDynamicOffersClient.toPurchaseMsisdn("795898572"));
        assertEquals("254708374149", SafaricomDynamicOffersClient.toPurchaseMsisdn("0708374149"));
        assertEquals("254117979906", SafaricomDynamicOffersClient.toPurchaseMsisdn("254117979906"));
        assertEquals("254117979906", SafaricomDynamicOffersClient.toPurchaseMsisdn("0117979906"));
    }

    @Test
    void convertsNationalMsisdn() {
        assertEquals("708374149", SafaricomDynamicOffersClient.toNationalMsisdn("254708374149"));
        assertEquals("795898572", SafaricomDynamicOffersClient.toNationalMsisdn("795898572"));
        assertEquals("117979906", SafaricomDynamicOffersClient.toNationalMsisdn("0117979906"));
    }

    @Test
    void convertsHeaderMsisdnToInternationalFormat() {
        assertEquals("254708374149", SafaricomDynamicOffersClient.toInternationalMsisdn("254708374149"));
        assertEquals("254795898572", SafaricomDynamicOffersClient.toInternationalMsisdn("795898572"));
        assertEquals("254708374149", SafaricomDynamicOffersClient.toInternationalMsisdn("0708374149"));
    }

    @Test
    void normalizesPaymentModes() {
        assertEquals("airtime", SafaricomDynamicOffersClient.normalizePaymentMode(null));
        assertEquals("airtime", SafaricomDynamicOffersClient.normalizePaymentMode("Airtime"));
        assertEquals("m-pesa", SafaricomDynamicOffersClient.normalizePaymentMode("M-PESA"));
        assertEquals("m-pesa", SafaricomDynamicOffersClient.normalizePaymentMode("mpesa"));
    }
}
