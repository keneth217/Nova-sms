package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<WalletTransaction> findByReference(String reference);

    Optional<WalletTransaction> findByCheckoutRequestId(String checkoutRequestId);

    Optional<WalletTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT t FROM WalletTransaction t
            WHERE t.organization.id = :organizationId
              AND (:type IS NULL OR t.type = :type)
              AND (:statusesEmpty = TRUE OR t.topupStatus IN :statuses)
            ORDER BY t.createdAt DESC
            """)
    Page<WalletTransaction> findByOrganizationFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("type") WalletTransactionType type,
            @Param("statuses") Collection<TopupStatus> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
            WHERE t.organization.id = :organizationId
              AND t.type = :type
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    BigDecimal sumAmountByOrgTypeAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("type") WalletTransactionType type,
            @Param("from") Instant from,
            @Param("to") Instant to);

    long countByTypeAndTopupStatus(WalletTransactionType type, TopupStatus topupStatus);
}
