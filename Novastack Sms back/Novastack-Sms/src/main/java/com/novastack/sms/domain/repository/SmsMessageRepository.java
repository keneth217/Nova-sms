package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.BillingStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, UUID> {

    @EntityGraph(attributePaths = "organization")
    Page<SmsMessage> findByOrganizationIdAndChannelOrderByCreatedAtDesc(
            UUID organizationId, MessageChannel channel, Pageable pageable);

    @EntityGraph(attributePaths = "organization")
    Page<SmsMessage> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "organization")
    Page<SmsMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<SmsMessage> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Optional<SmsMessage> findByProviderMessageId(String providerMessageId);

    long countByApiClientId(UUID apiClientId);

    long countByApiClientIdAndStatusIn(UUID apiClientId, Collection<MessageStatus> statuses);

    long countByApiClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID apiClientId, Instant from, Instant to);

    Optional<SmsMessage> findFirstByApiClientIdOrderByCreatedAtDesc(UUID apiClientId);

    List<SmsMessage> findByApiClientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            UUID apiClientId, Instant from);

    @Query("SELECT COALESCE(SUM(m.smsUnits), 0) FROM SmsMessage m WHERE m.apiClientId = :apiClientId")
    long sumUnitsByApiClientId(@Param("apiClientId") UUID apiClientId);

    List<SmsMessage> findByBatchIdAndOrganization_Id(UUID batchId, UUID organizationId);

    @Query("SELECT m FROM SmsMessage m JOIN FETCH m.organization WHERE m.id = :id")
    Optional<SmsMessage> findByIdWithOrganization(@Param("id") UUID id);

    @Query("""
            SELECT m FROM SmsMessage m JOIN FETCH m.organization
            WHERE m.batchId = :batchId AND m.status = :status
            ORDER BY m.createdAt ASC
            """)
    List<SmsMessage> findByBatchIdAndStatusWithOrganization(
            @Param("batchId") UUID batchId,
            @Param("status") MessageStatus status);

    @Query("""
            SELECT m FROM SmsMessage m JOIN FETCH m.organization
            WHERE m.status = :status AND m.scheduledAt <= :scheduledAt
            """)
    List<SmsMessage> findDueScheduledMessages(
            @Param("status") MessageStatus status,
            @Param("scheduledAt") Instant scheduledAt);

    List<SmsMessage> findByStatusAndScheduledAtLessThanEqual(MessageStatus status, Instant scheduledAt);

    long countByOrganizationIdAndChannelAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, MessageChannel channel, Instant from, Instant to);

    long countByOrganizationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, Instant from, Instant to);

    long countByOrganizationIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, MessageStatus status, Instant from, Instant to);

    @Query("""
            SELECT COALESCE(SUM(m.smsUnits), 0) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.channel = :channel
              AND m.status NOT IN (
                    com.novastack.sms.domain.enums.MessageStatus.FAILED,
                    com.novastack.sms.domain.enums.MessageStatus.REJECTED,
                    com.novastack.sms.domain.enums.MessageStatus.CANCELLED
              )
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    long sumUnitsByOrgChannelAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("channel") MessageChannel channel,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(m.cost), 0) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.channel = :channel
              AND m.status NOT IN (
                    com.novastack.sms.domain.enums.MessageStatus.FAILED,
                    com.novastack.sms.domain.enums.MessageStatus.REJECTED,
                    com.novastack.sms.domain.enums.MessageStatus.CANCELLED
              )
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    BigDecimal sumCostByOrgChannelAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("channel") MessageChannel channel,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT m FROM SmsMessage m JOIN FETCH m.organization
            WHERE m.provider = :provider
              AND m.providerMessageId IS NOT NULL
              AND m.status IN :statuses
              AND m.sentAt IS NOT NULL
              AND m.sentAt <= :sentBefore
            ORDER BY m.sentAt ASC
            """)
    List<SmsMessage> findForProviderStatusSync(
            @Param("provider") String provider,
            @Param("statuses") Collection<MessageStatus> statuses,
            @Param("sentBefore") Instant sentBefore,
            Pageable pageable);

    long countByOrganizationIdAndChannelAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, MessageChannel channel, Collection<MessageStatus> statuses, Instant from, Instant to);

    long countByOrganizationIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID organizationId, Collection<MessageStatus> statuses, Instant from, Instant to);

    @Query("""
            SELECT COUNT(m) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.channel = :channel
              AND m.status = :status
              AND m.createdAt >= :from AND m.createdAt < :to
            """)
    long countByOrgChannelStatusAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("channel") MessageChannel channel,
            @Param("status") MessageStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    long countByOrganizationIdAndChannelAndBillingStatus(
            UUID organizationId, MessageChannel channel, BillingStatus billingStatus);

    long countByChannelAndBillingStatus(MessageChannel channel, BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.smsUnits), 0) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.channel = :channel
              AND m.billingStatus = :billingStatus
            """)
    long sumUnitsByOrgChannelAndBillingStatus(
            @Param("organizationId") UUID organizationId,
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.cost), 0) FROM SmsMessage m
            WHERE m.organization.id = :organizationId
              AND m.channel = :channel
              AND m.billingStatus = :billingStatus
            """)
    BigDecimal sumCostByOrgChannelAndBillingStatus(
            @Param("organizationId") UUID organizationId,
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.smsUnits), 0) FROM SmsMessage m
            WHERE m.channel = :channel AND m.billingStatus = :billingStatus
            """)
    long sumUnitsByChannelAndBillingStatus(
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.cost), 0) FROM SmsMessage m
            WHERE m.channel = :channel AND m.billingStatus = :billingStatus
            """)
    BigDecimal sumCustomerRevenueByChannelAndBillingStatus(
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.providerCost), 0) FROM SmsMessage m
            WHERE m.channel = :channel AND m.billingStatus = :billingStatus
            """)
    BigDecimal sumProviderCostByChannelAndBillingStatus(
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);

    @Query("""
            SELECT COALESCE(SUM(m.grossMargin), 0) FROM SmsMessage m
            WHERE m.channel = :channel AND m.billingStatus = :billingStatus
            """)
    BigDecimal sumGrossMarginByChannelAndBillingStatus(
            @Param("channel") MessageChannel channel,
            @Param("billingStatus") BillingStatus billingStatus);
}
