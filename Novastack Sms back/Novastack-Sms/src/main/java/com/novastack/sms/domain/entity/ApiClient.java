package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.ApiPermission;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "api_clients", indexes = {
        @Index(name = "idx_api_clients_org", columnList = "organization_id"),
        @Index(name = "idx_api_clients_prefix", columnList = "api_key_prefix")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "client_code", nullable = false, unique = true, length = 64)
    private String clientCode;

    @Column(name = "api_key_hash", nullable = false, unique = true, length = 64)
    private String apiKeyHash;

    @Column(name = "api_key_prefix", nullable = false, length = 24)
    private String apiKeyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApiClientStatus status = ApiClientStatus.ACTIVE;

    @Column(name = "rate_limit_per_minute", nullable = false)
    @Builder.Default
    private int rateLimitPerMinute = 100;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_client_permissions", joinColumns = @JoinColumn(name = "api_client_id"))
    @Column(name = "permission", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<ApiPermission> permissions = new HashSet<>(EnumSet.of(
            ApiPermission.SMS_SEND,
            ApiPermission.SMS_BULK,
            ApiPermission.SMS_STATUS));

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isUsable() {
        if (status != ApiClientStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || Instant.now().isBefore(expiresAt);
    }
}
