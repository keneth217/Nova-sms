package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ContactResponse {

    private UUID id;
    private String phone;
    private String firstName;
    private String lastName;
    private String email;
    private List<UUID> groupIds;
    private List<String> groupNames;
    private Instant createdAt;
}
