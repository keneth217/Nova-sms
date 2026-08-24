package com.novastack.sms.domain.entity;

import com.novastack.sms.domain.enums.TransactionStatusQueryState;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mpesa_transaction_status_query", indexes = {
        @Index(name = "idx_mpesa_txn_status_receipt", columnList = "mpesa_receipt"),
        @Index(name = "idx_mpesa_txn_status_originator", columnList = "originator_conversation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MpesaTransactionStatusQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mpesa_receipt", nullable = false, length = 50)
    private String mpesaReceipt;

    @Column(name = "originator_conversation_id", length = 64)
    private String originatorConversationId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatusQueryState status = TransactionStatusQueryState.PENDING;

    @Column(name = "result_code", length = 32)
    private String resultCode;

    @Column(name = "result_desc", length = 500)
    private String resultDesc;

    @Column(precision = 14, scale = 4)
    private BigDecimal amount;

    @Column(name = "bill_ref", length = 32)
    private String billRef;

    @Column(name = "transaction_status", length = 40)
    private String transactionStatus;

    @Column(name = "raw_result", length = 4000)
    private String rawResult;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
