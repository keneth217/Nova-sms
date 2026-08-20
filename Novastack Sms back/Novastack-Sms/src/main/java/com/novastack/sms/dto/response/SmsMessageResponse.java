package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SmsMessageResponse {

    private UUID id;
    private String messageId;
    private UUID organizationId;
    private String organizationName;
    private UUID apiClientId;
    private String recipient;
    private String content;
    private MessageChannel channel;
    private String senderId;
    private MessageStatus status;
    private BigDecimal cost;
    private Integer smsUnits;
    private String encoding;
    private Integer characterCount;
    private BigDecimal unitPrice;
    private String currency;
    private String provider;
    private String providerMessageId;
    private UUID batchId;
    private Instant scheduledAt;
    private Instant createdAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private String failureReason;
}
