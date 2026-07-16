package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ContactGroupResponse {

    private UUID id;
    private String name;
    private String description;
    private long contactCount;
    private Instant createdAt;
}
