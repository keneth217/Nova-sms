package com.novastack.sms.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.novastack.sms.domain.enums.PaymentMethod;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_tx_org", columnList = "organization_id"),
        @Index(name = "idx_wallet_tx_ref", columnList = "reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnore
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 14, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(length = 255)
    private String description;

    @Column(name = "mpesa_receipt", length = 50)
    private String mpesaReceipt;

    @Column(name = "mpesa_transaction_date", length = 20)
    private String mpesaTransactionDate;

    /** STK payer MSISDN (254…) or C2B SHA-256 MSISDN hash (64 hex chars). */
    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "checkout_request_id", length = 64)
    private String checkoutRequestId;

    @Column(name = "merchant_request_id", length = 64)
    private String merchantRequestId;

    @Column(name = "result_code", length = 20)
    private String resultCode;

    @Column(name = "result_desc", length = 255)
    private String resultDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "topup_status", length = 20)
    private TopupStatus topupStatus;

    @Column(name = "callback_received", nullable = false)
    @Builder.Default
    private boolean callbackReceived = false;

    @Column(name = "wallet_credited", nullable = false)
    @Builder.Default
    private boolean walletCredited = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "bill_ref", length = 32)
    private String billRef;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
