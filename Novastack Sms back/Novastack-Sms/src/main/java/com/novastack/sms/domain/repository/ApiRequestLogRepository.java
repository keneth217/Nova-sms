package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ApiRequestLog;
import com.novastack.sms.domain.enums.ApiRequestOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, UUID> {

    Page<ApiRequestLog> findByApiClientIdOrderByCreatedAtDesc(UUID apiClientId, Pageable pageable);

    long countByApiClientIdAndCreatedAtGreaterThanEqual(UUID apiClientId, Instant from);

    long countByApiClientIdAndOutcomeAndCreatedAtGreaterThanEqual(
            UUID apiClientId, ApiRequestOutcome outcome, Instant from);

    long countByApiClientIdAndResourceCategoryAndCreatedAtGreaterThanEqual(
            UUID apiClientId, String resourceCategory, Instant from);

    long countByApiClientIdAndStatusGreaterThanEqualAndCreatedAtGreaterThanEqual(
            UUID apiClientId, int status, Instant from);

    @Query("""
            SELECT AVG(l.durationMs) FROM ApiRequestLog l
            WHERE l.apiClientId = :apiClientId AND l.createdAt >= :from
            """)
    Double averageDurationMs(@Param("apiClientId") UUID apiClientId, @Param("from") Instant from);

    @Query("""
            SELECT l.path, COUNT(l) FROM ApiRequestLog l
            WHERE l.apiClientId = :apiClientId AND l.createdAt >= :from
            GROUP BY l.path
            ORDER BY COUNT(l) DESC
            """)
    List<Object[]> topPaths(@Param("apiClientId") UUID apiClientId, @Param("from") Instant from, Pageable pageable);

    @Query("""
            SELECT l.apiClientId, COUNT(l) FROM ApiRequestLog l
            WHERE l.createdAt >= :from
            GROUP BY l.apiClientId
            ORDER BY COUNT(l) DESC
            """)
    List<Object[]> requestCountsByClient(@Param("from") Instant from);

    @Query("""
            SELECT l.apiClientId, l.outcome, COUNT(l) FROM ApiRequestLog l
            WHERE l.createdAt >= :from
            GROUP BY l.apiClientId, l.outcome
            """)
    List<Object[]> outcomeCountsByClient(@Param("from") Instant from);

    @Query("""
            SELECT l.apiClientId, l.resourceCategory, COUNT(l) FROM ApiRequestLog l
            WHERE l.createdAt >= :from
            GROUP BY l.apiClientId, l.resourceCategory
            """)
    List<Object[]> categoryCountsByClient(@Param("from") Instant from);

    List<ApiRequestLog> findByApiClientIdAndCreatedAtGreaterThanEqual(UUID apiClientId, Instant from);

    List<ApiRequestLog> findByApiClientIdInAndCreatedAtGreaterThanEqual(
            Collection<UUID> apiClientIds, Instant from);

    long countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
            UUID apiClientId, String path, Instant from);

    @Query("""
            SELECT COUNT(l) FROM ApiRequestLog l
            WHERE l.apiClientId = :apiClientId AND l.createdAt >= :from
              AND (l.path LIKE '%/mpesa/%/status' OR l.path LIKE '%/mpesa/transactions/{id}')
            """)
    long countMpesaStatusCalls(@Param("apiClientId") UUID apiClientId, @Param("from") Instant from);
}
