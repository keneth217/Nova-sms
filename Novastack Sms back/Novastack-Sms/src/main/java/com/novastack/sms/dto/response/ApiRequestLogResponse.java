package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.ApiRequestOutcome;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ApiRequestLogResponse {

    private UUID id;
    private String requestId;
    private String method;
    private String path;
    private String permission;
    private String resourceCategory;
    private int status;
    private ApiRequestOutcome outcome;
    private int durationMs;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
