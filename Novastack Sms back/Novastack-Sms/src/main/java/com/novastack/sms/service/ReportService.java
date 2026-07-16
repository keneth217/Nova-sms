package com.novastack.sms.service;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SmsMessageRepository smsMessageRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public DashboardReportResponse dashboard(UUID organizationId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();

        long smsToday = smsMessageRepository.countByOrganizationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId, startOfDay, now);
        long smsMonth = smsMessageRepository.countByOrganizationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                organizationId, startOfMonth, now);

        long delivered = smsMessageRepository.countByOrgStatusAndPeriod(
                organizationId, MessageStatus.DELIVERED, startOfMonth, now);
        long failed = smsMessageRepository.countByOrgStatusAndPeriod(
                organizationId, MessageStatus.FAILED, startOfMonth, now);
        long sent = smsMessageRepository.countByOrgStatusAndPeriod(
                organizationId, MessageStatus.SENT, startOfMonth, now);

        long deliverableBase = delivered + failed + sent;
        double deliveryRate = deliverableBase == 0 ? 0.0
                : BigDecimal.valueOf(delivered * 100.0 / deliverableBase)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        BigDecimal balance = walletRepository.findByOrganizationId(organizationId)
                .map(wallet -> wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        BigDecimal usageToday = walletTransactionRepository.sumAmountByOrgTypeAndPeriod(
                organizationId, WalletTransactionType.SMS_DEBIT, startOfDay, now);
        BigDecimal usageMonth = walletTransactionRepository.sumAmountByOrgTypeAndPeriod(
                organizationId, WalletTransactionType.SMS_DEBIT, startOfMonth, now);

        BigDecimal costToday = smsMessageRepository.sumCostByOrgAndPeriod(organizationId, startOfDay, now);
        BigDecimal costMonth = smsMessageRepository.sumCostByOrgAndPeriod(organizationId, startOfMonth, now);

        return DashboardReportResponse.builder()
                .smsSentToday(smsToday)
                .smsSentThisMonth(smsMonth)
                .deliveredCount(delivered)
                .failedCount(failed)
                .deliveryRate(deliveryRate)
                .walletBalance(balance)
                .walletUsageToday(usageToday)
                .walletUsageThisMonth(usageMonth)
                .costToday(costToday)
                .costThisMonth(costMonth)
                .build();
    }
}
