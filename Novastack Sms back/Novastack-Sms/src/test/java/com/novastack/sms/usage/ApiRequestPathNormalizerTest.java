package com.novastack.sms.usage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiRequestPathNormalizerTest {

    @Test
    void stripsQueryAndContextAndUuids() {
        assertEquals(
                "/api/v1/mpesa/transactions/{id}/status",
                ApiRequestPathNormalizer.normalize(
                        "/novasms/api/v1/mpesa/transactions/11111111-1111-1111-1111-111111111111/status?foo=1"));
        assertEquals("/api/v1/sms/send", ApiRequestPathNormalizer.normalize("/api/v1/sms/send"));
        assertEquals("SMS", ApiRequestPathNormalizer.category("/api/v1/sms/bulk"));
        assertEquals("MPESA", ApiRequestPathNormalizer.category("/api/v1/mpesa/stkpush"));
        assertEquals("WALLET", ApiRequestPathNormalizer.category("/api/v1/wallet/topup"));
    }
}
