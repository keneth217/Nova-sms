package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AnnouncementResponse {

    private boolean enabled;
    private String label;
    private String title;
    private String body;
    private String tone;
    private Instant updatedAt;
}
