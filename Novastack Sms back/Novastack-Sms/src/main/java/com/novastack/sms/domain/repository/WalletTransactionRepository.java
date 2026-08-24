package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    Optional<WalletTransaction> findByReferenceIgnoreCase(String reference);

    Optional<WalletTransaction> findByMpesaReceipt(String mpesaReceipt);

    Optional<WalletTransaction> findByMpesaReceiptIgnoreCase(String mpesaReceipt);

    Optional<WalletTransaction> findByCheckoutRequestId(String checkoutRequestId);

    Optional<WalletTransaction> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * STK rows this C2B confirmation may attach to: uncredited first, then
     * already-credited rows that still have no receipt (STK Query completed them).
     */
    @Query("""
            SELECT t FROM WalletTransaction t
            WHERE t.organization.id = :organizationId
              AND t.type = com.novastack.sms.domain.enums.WalletTransactionType.TOPUP
              AND t.amount = :amount
              AND t.createdAt >= :since
              AND (
                    t.walletCredited = false
                    OR t.mpesaReceipt IS NULL
                    OR t.mpesaReceipt = ''
                  )
            ORDER BY CASE WHEN t.walletCredited = false THEN 0 ELSE 1 END, t.createdAt DESC
            """)
    List<WalletTransaction> findC2bAttachCandidates(
            @Param("organizationId") UUID organizationId,
            @Param("amount") BigDecimal amount,
            @Param("since") Instant since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTransaction t WHERE t.checkoutRequestId = :checkoutRequestId")
    Optional<WalletTransaction> findByCheckoutRequestIdForUpdate(@Param("checkoutRequestId") String checkoutRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTransaction t WHERE t.id = :id AND t.organization.id = :organizationId")
    Optional<WalletTransaction> findByIdAndOrganizationIdForUpdate(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);

    @Query(
            value = """
                    SELECT t FROM WalletTransaction t
                    JOIN FETCH t.organization
                    WHERE t.organization.id = :organizationId
                      AND (:type IS NULL OR t.type = :type)
                      AND (:statusesEmpty = TRUE OR t.topupStatus IN :statuses)
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM WalletTransaction t
                    WHERE t.organization.id = :organizationId
                      AND (:type IS NULL OR t.type = :type)
                      AND (:statusesEmpty = TRUE OR t.topupStatus IN :statuses)
                    """)
    Page<WalletTransaction> findByOrganizationFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("type") WalletTransactionType type,
            @Param("statuses") Collection<TopupStatus> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            Pageable pageable);

    @Query(
            value = """
                    SELECT t FROM WalletTransaction t
                    JOIN FETCH t.organization
                    WHERE (:type IS NULL OR t.type = :type)
                      AND (:statusesEmpty = TRUE OR t.topupStatus IN :statuses)
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM WalletTransaction t
                    WHERE (:type IS NULL OR t.type = :type)
                      AND (:statusesEmpty = TRUE OR t.topupStatus IN :statuses)
                    """)
    Page<WalletTransaction> findPlatformFiltered(
            @Param("type") WalletTransactionType type,
            @Param("statuses") Collection<TopupStatus> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            Pageable pageable);

    @Query(
            value = """
                    SELECT t FROM WalletTransaction t
                    JOIN FETCH t.organization
                    WHERE t.organization.id = :organizationId
                      AND t.type = com.novastack.sms.domain.enums.WalletTransactionType.TOPUP
                      AND (
                            t.paymentMethod = com.novastack.sms.domain.enums.PaymentMethod.PAYBILL
                            OR (
                                t.paymentMethod IS NULL
                                AND (t.checkoutRequestId IS NULL OR t.checkoutRequestId = '')
                            )
                          )
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM WalletTransaction t
                    WHERE t.organization.id = :organizationId
                      AND t.type = com.novastack.sms.domain.enums.WalletTransactionType.TOPUP
                      AND (
                            t.paymentMethod = com.novastack.sms.domain.enums.PaymentMethod.PAYBILL
                            OR (
                                t.paymentMethod IS NULL
                                AND (t.checkoutRequestId IS NULL OR t.checkoutRequestId = '')
                            )
                          )
                    """)
    Page<WalletTransaction> findC2bTopupsByOrganization(
            @Param("organizationId") UUID organizationId,
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

    long countByApiClientIdAndTypeAndCheckoutRequestIdIsNotNull(UUID apiClientId, WalletTransactionType type);

    long countByApiClientIdAndTypeAndWalletCreditedTrue(UUID apiClientId, WalletTransactionType type);
}
