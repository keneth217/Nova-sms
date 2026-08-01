package com.novastack.sms.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_bundle_callback_logs", indexes = {
        @Index(name = "idx_dbcl_reference", columnList = "reference"),
        @Index(name = "idx_dbcl_received", columnList = "received_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataBundleCallbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 40)
    private String reference;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "process_error", length = 500)
    private String processError;
}
