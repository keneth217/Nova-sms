package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_delivery_reports", indexes = {
        @Index(name = "idx_dlr_message", columnList = "sms_message_id"),
        @Index(name = "idx_dlr_provider_id", columnList = "provider_message_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsDeliveryReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sms_message_id", nullable = false)
    private SmsMessage smsMessage;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(length = 20)
    private String networkCode;

    @Column(length = 20)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
