package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.DataBundleTransaction;
import com.novastack.sms.domain.enums.BundleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DataBundleTransactionRepository extends JpaRepository<DataBundleTransaction, UUID> {

    Optional<DataBundleTransaction> findByReference(String reference);

    Optional<DataBundleTransaction> findByOrganizationIdAndReference(UUID organizationId, String reference);

    boolean existsByOrganizationIdAndReference(UUID organizationId, String reference);

    Page<DataBundleTransaction> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("""
            SELECT t FROM DataBundleTransaction t
            WHERE t.organization.id = :organizationId
              AND (:status IS NULL OR t.status = :status)
              AND (:phone IS NULL OR t.phoneNumber LIKE CONCAT('%', :phone, '%'))
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt < :to)
            ORDER BY t.createdAt DESC
            """)
    Page<DataBundleTransaction> search(
            @Param("organizationId") UUID organizationId,
            @Param("status") BundleStatus status,
            @Param("phone") String phone,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByOrganizationIdAndStatus(UUID organizationId, BundleStatus status);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM DataBundleTransaction t
            WHERE t.organization.id = :organizationId AND t.status = :status
            """)
    BigDecimal sumAmountByOrganizationIdAndStatus(
            @Param("organizationId") UUID organizationId,
            @Param("status") BundleStatus status);
}
