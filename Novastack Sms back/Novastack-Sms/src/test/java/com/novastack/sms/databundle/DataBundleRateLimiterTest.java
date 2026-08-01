package com.novastack.sms.databundle;

import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataBundleRateLimiterTest {

    @Test
    void allowsRequestsWithinLimit() {
        DataBundleRateLimiter limiter = new DataBundleRateLimiter();
        UUID orgId = UUID.randomUUID();
        assertDoesNotThrow(() -> {
            limiter.check(orgId, 3);
            limiter.check(orgId, 3);
            limiter.check(orgId, 3);
        });
    }

    @Test
    void blocksWhenLimitExceeded() {
        DataBundleRateLimiter limiter = new DataBundleRateLimiter();
        UUID orgId = UUID.randomUUID();
        limiter.check(orgId, 2);
        limiter.check(orgId, 2);
        ApiException ex = assertThrows(ApiException.class, () -> limiter.check(orgId, 2));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }
}
