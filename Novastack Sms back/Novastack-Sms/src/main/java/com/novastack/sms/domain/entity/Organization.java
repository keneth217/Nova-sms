package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationBillingModel;
import com.novastack.sms.domain.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations", indexes = {
        @Index(name = "idx_org_api_key", columnList = "api_key", unique = true),
        @Index(name = "idx_org_email", columnList = "email", unique = true),
        @Index(name = "idx_org_account_type", columnList = "account_type"),
        @Index(name = "idx_org_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "mpesa_account_ref", unique = true, length = 12)
    private String mpesaAccountRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private OrganizationAccountType accountType = OrganizationAccountType.BUSINESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_model", nullable = false, length = 20)
    @Builder.Default
    private OrganizationBillingModel billingModel = OrganizationBillingModel.PREPAID;

    /** When set (EVENT accounts), login and sending stop after this instant. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sms_cost", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal smsCost = new BigDecimal("1.00");

    @Column(name = "at_username", length = 100)
    private String atUsername;

    @Column(name = "at_api_key", length = 255)
    private String atApiKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isEventAccount() {
        return accountType == OrganizationAccountType.EVENT;
    }

    public boolean isExpired() {
        if (status == OrganizationStatus.EXPIRED) {
            return true;
        }
        return isEventAccount() && expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
