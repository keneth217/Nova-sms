package com.novastack.sms.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.novastack.sms.domain.enums.BundleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_bundle_transactions", indexes = {
        @Index(name = "idx_dbt_org_created", columnList = "organization_id,created_at"),
        @Index(name = "idx_dbt_phone", columnList = "phone_number"),
        @Index(name = "idx_dbt_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataBundleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "offer_id", nullable = false, length = 64)
    private String offerId;

    @Column(name = "offer_name", nullable = false, length = 150)
    private String offerName;

    @Column(length = 40)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BundleStatus status = BundleStatus.PENDING;

    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    @Column(name = "provider_request_id", length = 100)
    private String providerRequestId;

    @Column(name = "response_code", length = 40)
    private String responseCode;

    @Column(name = "response_description", length = 500)
    private String responseDescription;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "wallet_debited", nullable = false)
    @Builder.Default
    private boolean walletDebited = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
