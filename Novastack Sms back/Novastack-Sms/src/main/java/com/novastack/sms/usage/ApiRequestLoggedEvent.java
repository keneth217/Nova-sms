package com.novastack.sms.usage;

import com.novastack.sms.domain.enums.ApiRequestOutcome;

import java.time.Instant;
import java.util.UUID;

public record ApiRequestLoggedEvent(
        UUID apiClientId,
        UUID organizationId,
        String requestId,
        String method,
        String path,
        String permission,
        String resourceCategory,
        int status,
        ApiRequestOutcome outcome,
        int durationMs,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
