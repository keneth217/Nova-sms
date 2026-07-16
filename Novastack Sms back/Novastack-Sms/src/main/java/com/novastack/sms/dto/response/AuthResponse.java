package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UUID userId;
    private String email;
    private String fullName;
    private UserRole role;
    private UUID organizationId;
    private OrganizationAccountType accountType;
    private Instant expiresAt;
}
