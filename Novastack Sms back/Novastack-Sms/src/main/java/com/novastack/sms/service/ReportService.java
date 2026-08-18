package com.novastack.sms.service;

import com.novastack.sms.domain.enums.BillingStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.response.DashboardReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SmsMessageRepository smsMessageRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BillingSettingsService billingSettingsService;
    private final SmsBillingCalculator smsBillingCalculator;

    @Transactional(readOnly = true)
    public DashboardReportResponse dashboard(UUID organizationId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();

        long smsToday = smsMessageRepository.countByOrganizationIdAndChannelAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId, MessageChannel.SMS, startOfDay, now);
        long smsMonth = smsMessageRepository.countByOrganizationIdAndChannelAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId, MessageChannel.SMS, startOfMonth, now);

        long delivered = smsMessageRepository.countByOrgChannelStatusAndPeriod(
                organizationId, MessageChannel.SMS, MessageStatus.DELIVERED, startOfMonth, now);
        long failed = smsMessageRepository.countByOrganizationIdAndChannelAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId,
                MessageChannel.SMS,
                List.of(MessageStatus.FAILED, MessageStatus.REJECTED, MessageStatus.CANCELLED),
                startOfMonth, now);
        long pending = smsMessageRepository.countByOrganizationIdAndChannelAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId,
                MessageChannel.SMS,
                List.of(MessageStatus.PENDING, MessageStatus.ACCEPTED, MessageStatus.SENT, MessageStatus.SCHEDULED),
                startOfMonth, now);

        long finalized = delivered + failed;
        double deliveryRate = finalized == 0 ? 0.0
                : BigDecimal.valueOf(delivered * 100.0 / finalized)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        BigDecimal balance = walletRepository.findByOrganizationId(organizationId)
                .map(wallet -> wallet.getBalance())
                .orElse(BigDecimal.ZERO);
        BigDecimal smsPrice = billingSettingsService.customerPrice();
        long availableSms = smsBillingCalculator.availableSms(balance);
        long smsSent = smsMessageRepository.countByOrganizationIdAndChannelAndBillingStatus(
                organizationId, MessageChannel.SMS, BillingStatus.CHARGED);
        long lifetimeUnits = smsMessageRepository.sumUnitsByOrgChannelAndBillingStatus(
                organizationId, MessageChannel.SMS, BillingStatus.CHARGED);
        BigDecimal lifetimeSpent = smsMessageRepository.sumCostByOrgChannelAndBillingStatus(
                organizationId, MessageChannel.SMS, BillingStatus.CHARGED);

        BigDecimal usageToday = walletTransactionRepository.sumAmountByOrgTypeAndPeriod(
                organizationId, WalletTransactionType.SMS_DEBIT, startOfDay, now);
        BigDecimal usageMonth = walletTransactionRepository.sumAmountByOrgTypeAndPeriod(
                organizationId, WalletTransactionType.SMS_DEBIT, startOfMonth, now);

        BigDecimal costToday = smsMessageRepository.sumCostByOrgChannelAndPeriod(
                organizationId, MessageChannel.SMS, startOfDay, now);
        BigDecimal costMonth = smsMessageRepository.sumCostByOrgChannelAndPeriod(
                organizationId, MessageChannel.SMS, startOfMonth, now);

        return DashboardReportResponse.builder()
                .smsSentToday(smsToday)
                .smsSentThisMonth(smsMonth)
                .deliveredCount(delivered)
                .failedCount(failed)
                .pendingCount(pending)
                .deliveryRate(deliveryRate)
                .walletBalance(balance)
                .walletUsageToday(usageToday)
                .walletUsageThisMonth(usageMonth)
                .costToday(costToday)
                .costThisMonth(costMonth)
                .smsPrice(smsPrice)
                .availableSms(availableSms)
                .smsSent(smsSent)
                .totalSmsUnits(lifetimeUnits)
                .totalAmountSpent(lifetimeSpent != null ? lifetimeSpent : BigDecimal.ZERO)
                .build();
    }
}
