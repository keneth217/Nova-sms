package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.ApiRequestOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_request_logs", indexes = {
        @Index(name = "idx_api_req_client_created", columnList = "api_client_id,created_at"),
        @Index(name = "idx_api_req_org_created", columnList = "organization_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "api_client_id", nullable = false)
    private UUID apiClientId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "request_id", nullable = false, length = 40)
    private String requestId;

    @Column(nullable = false, length = 12)
    private String method;

    @Column(nullable = false, length = 180)
    private String path;

    @Column(length = 32)
    private String permission;

    @Column(name = "resource_category", nullable = false, length = 16)
    private String resourceCategory;

    @Column(nullable = false)
    private int status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiRequestOutcome outcome;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 180)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
