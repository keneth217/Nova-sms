package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.MpesaTransactionStatusQuery;
import com.novastack.sms.domain.enums.TransactionStatusQueryState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MpesaTransactionStatusQueryRepository extends JpaRepository<MpesaTransactionStatusQuery, UUID> {

    Optional<MpesaTransactionStatusQuery> findFirstByMpesaReceiptIgnoreCaseAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String mpesaReceipt,
            TransactionStatusQueryState status,
            Instant createdAfter);

    Optional<MpesaTransactionStatusQuery> findFirstByOriginatorConversationId(String originatorConversationId);

    Optional<MpesaTransactionStatusQuery> findFirstByMpesaReceiptIgnoreCaseOrderByCreatedAtDesc(String mpesaReceipt);
}
