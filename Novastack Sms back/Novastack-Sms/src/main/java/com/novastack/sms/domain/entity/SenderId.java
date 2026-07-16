package com.novastack.sms.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.novastack.sms.domain.enums.SenderIdStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sender_ids", indexes = {
        @Index(name = "idx_sender_org_name", columnList = "organization_id,sender_name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderId {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private Organization organization;

    @Column(name = "sender_name", nullable = false, length = 11)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SenderIdStatus status = SenderIdStatus.PENDING;

    @Column(name = "is_platform_default", nullable = false)
    @Builder.Default
    private boolean platformDefault = false;

    @Column(length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
