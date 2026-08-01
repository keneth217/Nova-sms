package com.novastack.sms.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_bundle_offers", indexes = {
        @Index(name = "idx_dbo_offer_id", columnList = "offer_id"),
        @Index(name = "idx_dbo_org_category", columnList = "organization_id,category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataBundleOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private Organization organization;

    @Column(name = "offer_id", nullable = false, length = 64)
    private String offerId;

    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(name = "offer_name", nullable = false, length = 150)
    private String offerName;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "offer_source", length = 40)
    private String offerSource;

    @Column(name = "parent_offer_id", length = 64)
    private String parentOfferId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "resource_amount", length = 40)
    private String resourceAmount;

    @Column(length = 80)
    private String validity;

    @Column(length = 500)
    private String description;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
