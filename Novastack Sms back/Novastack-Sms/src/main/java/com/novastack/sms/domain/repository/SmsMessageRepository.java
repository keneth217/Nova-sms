package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
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

public interface SmsMessageRepository extends JpaRepository<SmsMessage, UUID> {

    Page<SmsMessage> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<SmsMessage> findByProviderMessageId(String providerMessageId);

    @Query("SELECT m FROM SmsMessage m JOIN FETCH m.organization WHERE m.id = :id")
    Optional<SmsMessage> findByIdWithOrganization(@Param("id") UUID id);

    @Query("""
            SELECT m FROM SmsMessage m JOIN FETCH m.organization
            WHERE m.status = :status AND m.scheduledAt <= :scheduledAt
            """)
    List<SmsMessage> findDueScheduledMessages(
            @Param("status") MessageStatus status,
            @Param("scheduledAt") Instant scheduledAt);

    List<SmsMessage> findByStatusAndScheduledAtLessThanEqual(MessageStatus status, Instant scheduledAt);

    long countByOrganizationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, Instant from, Instant to);

    long countByOrganizationIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, MessageStatus status, Instant from, Instant to);

    @Query("""
            SELECT COALESCE(SUM(m.cost), 0) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.status <> com.novastack.sms.domain.enums.MessageStatus.FAILED
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    BigDecimal sumCostByOrgAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT COUNT(m) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.status = :status
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    long countByOrgStatusAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("status") MessageStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
