package com.novastack.sms.dto.request;

import com.novastack.sms.domain.enums.ApiPermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class UpdateApiClientRequest {

    @Size(max = 120)
    private String name;

    private Set<ApiPermission> permissions;

    @Min(1)
    @Max(10_000)
    private Integer rateLimitPerMinute;

    private Instant expiresAt;
}
