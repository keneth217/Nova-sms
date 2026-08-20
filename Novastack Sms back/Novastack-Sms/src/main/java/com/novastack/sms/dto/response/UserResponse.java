package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String phone;
    private String fullName;
    private UserRole role;
    private boolean enabled;
    private UUID organizationId;
    private String organizationName;
    private Instant createdAt;
}
