package com.novastack.sms.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class ScheduleSmsRequest {

    private List<@NotBlank @Size(max = 20) String> recipients;

    @NotBlank
    @Size(max = 1600)
    private String message;

    @Size(max = 11)
    private String senderId;

    @NotNull
    @Future
    private Instant scheduledAt;

    private UUID groupId;
}
