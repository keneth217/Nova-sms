package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.PaybillCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaybillCollectionRepository extends JpaRepository<PaybillCollection, UUID> {

    boolean existsByMpesaReceipt(String mpesaReceipt);

    boolean existsByMpesaReceiptIgnoreCase(String mpesaReceipt);

    Optional<PaybillCollection> findByMpesaReceipt(String mpesaReceipt);

    Optional<PaybillCollection> findByMpesaReceiptIgnoreCase(String mpesaReceipt);

    Page<PaybillCollection> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PaybillCollection> findByBillRefIgnoreCaseOrderByCreatedAtDesc(String billRef, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant since);

    long countByBillRefIgnoreCase(String billRef);

    long countByBillRefIgnoreCaseAndCreatedAtGreaterThanEqual(String billRef, Instant since);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM PaybillCollection c")
    BigDecimal sumAmount();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM PaybillCollection c WHERE c.createdAt >= :since")
    BigDecimal sumAmountSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM PaybillCollection c WHERE UPPER(c.billRef) = UPPER(:billRef)")
    BigDecimal sumAmountByBillRef(@Param("billRef") String billRef);

    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM PaybillCollection c
            WHERE UPPER(c.billRef) = UPPER(:billRef) AND c.createdAt >= :since
            """)
    BigDecimal sumAmountByBillRefSince(@Param("billRef") String billRef, @Param("since") Instant since);

    @Query("""
            SELECT UPPER(c.billRef), COUNT(c), COALESCE(SUM(c.amount), 0)
            FROM PaybillCollection c
            GROUP BY UPPER(c.billRef)
            ORDER BY UPPER(c.billRef)
            """)
    List<Object[]> totalsByBillRef();
}
