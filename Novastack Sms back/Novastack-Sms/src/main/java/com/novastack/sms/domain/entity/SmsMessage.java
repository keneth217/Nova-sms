package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_messages", indexes = {
        @Index(name = "idx_sms_org_created", columnList = "organization_id,created_at"),
        @Index(name = "idx_sms_status", columnList = "status"),
        @Index(name = "idx_sms_provider_id", columnList = "provider_message_id"),
        @Index(name = "idx_sms_scheduled", columnList = "scheduled_at,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "recipient", nullable = false, length = 20)
    private String recipient;

    @Column(nullable = false, length = 1600)
    private String content;

    @Column(name = "sender_id", nullable = false, length = 11)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal cost;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
