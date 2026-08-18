package com.novastack.sms.security;

import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiClientRateLimiterTest {

    @Test
    void allowsUpToLimitThenReturns429() {
        ApiClientRateLimiter limiter = new ApiClientRateLimiter();
        UUID clientId = UUID.randomUUID();
        assertDoesNotThrow(() -> limiter.check(clientId, 2));
        assertDoesNotThrow(() -> limiter.check(clientId, 2));
        ApiException ex = assertThrows(ApiException.class, () -> limiter.check(clientId, 2));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }
}
