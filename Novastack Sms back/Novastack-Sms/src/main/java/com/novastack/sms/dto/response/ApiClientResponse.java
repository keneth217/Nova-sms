package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.ApiPermission;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class ApiClientResponse {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private String name;
    private String clientCode;
    private String apiKeyPrefix;
    private ApiClientStatus status;
    private Set<ApiPermission> permissions;
    private int rateLimitPerMinute;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private Instant createdAt;
}
