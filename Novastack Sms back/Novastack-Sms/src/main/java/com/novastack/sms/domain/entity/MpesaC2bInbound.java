package com.novastack.sms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mpesa_c2b_inbound", indexes = {
        @Index(name = "idx_mpesa_c2b_inbound_credited", columnList = "credited")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpesaC2bInbound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mpesa_receipt", nullable = false, unique = true, length = 50)
    private String mpesaReceipt;

    @Column(name = "bill_ref", length = 32)
    private String billRef;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "mpesa_transaction_date", length = 20)
    private String mpesaTransactionDate;

    @Column(length = 4000)
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private boolean credited = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
