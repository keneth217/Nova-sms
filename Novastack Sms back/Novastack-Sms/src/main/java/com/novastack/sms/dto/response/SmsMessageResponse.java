package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.MessageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SmsMessageResponse {

    private UUID id;
    private String recipient;
    private String content;
    private String senderId;
    private MessageStatus status;
    private BigDecimal cost;
    private UUID batchId;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private String failureReason;
}
