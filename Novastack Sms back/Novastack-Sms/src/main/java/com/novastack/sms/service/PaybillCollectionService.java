package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PaybillCollection;
import com.novastack.sms.domain.repository.PaybillCollectionRepository;
import com.novastack.sms.dto.response.PaybillCollectionDashboardResponse;
import com.novastack.sms.dto.response.PaybillCollectionDashboardResponse.AccountStat;
import com.novastack.sms.dto.response.PaybillCollectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaybillCollectionService {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    private final PaybillCollectionRepository paybillCollectionRepository;
    private final AppProperties appProperties;
    private final OrgNotificationService orgNotificationService;
    private final SmsSettingsService smsSettingsService;

    public boolean isCollectionAccount(String billRef) {
        if (billRef == null || billRef.isBlank()) {
            return false;
        }
        String normalized = billRef.trim().toUpperCase(Locale.ROOT);
        return collectionAccounts().contains(normalized);
    }

    public boolean existsByReceipt(String receipt) {
        return receipt != null && paybillCollectionRepository.existsByMpesaReceipt(receipt);
    }

    public Optional<PaybillCollection> findByReceipt(String receipt) {
        if (receipt == null || receipt.isBlank()) {
            return Optional.empty();
        }
        return paybillCollectionRepository.findByMpesaReceiptIgnoreCase(receipt.trim());
    }

    @Transactional
    public void record(
            String receipt,
            BigDecimal amount,
            String billRef,
            String phoneNumber,
            String transactionDate,
            String firstName,
            String middleName,
            String lastName) {
        if (receipt == null || amount == null || billRef == null) {
            return;
        }
        if (paybillCollectionRepository.existsByMpesaReceipt(receipt)) {
            log.info("Duplicate collection receipt ignored receipt={} billRef={}", receipt, billRef);
            return;
        }
        String normalizedRef = billRef.trim().toUpperCase(Locale.ROOT);
        paybillCollectionRepository.save(PaybillCollection.builder()
                .billRef(normalizedRef)
                .amount(amount)
                .mpesaReceipt(receipt)
                .phoneNumber(phoneNumber)
                .mpesaTransactionDate(transactionDate)
                .payerName(joinName(firstName, middleName, lastName))
                .build());
        log.info("Paybill collection recorded billRef={} amount={} receipt={}", normalizedRef, amount, receipt);
        orgNotificationService.notifyCollectionReceived(
                normalizedRef, amount, firstName, middleName, lastName, receipt);
    }

    @Transactional(readOnly = true)
    public PaybillCollectionDashboardResponse dashboard(String billRefFilter, Pageable pageable) {
        String filter = billRefFilter == null || billRefFilter.isBlank()
                ? null
                : billRefFilter.trim().toUpperCase(Locale.ROOT);
        Instant startOfToday = LocalDate.now(NAIROBI).atStartOfDay(NAIROBI).toInstant();
        Instant startOfMonth = YearMonth.now(NAIROBI).atDay(1).atStartOfDay(NAIROBI).toInstant();

        Page<PaybillCollection> recent = filter == null
                ? paybillCollectionRepository.findAllByOrderByCreatedAtDesc(pageable)
                : paybillCollectionRepository.findByBillRefIgnoreCaseOrderByCreatedAtDesc(filter, pageable);

        BigDecimal totalAmount;
        long totalCount;
        BigDecimal todayAmount;
        long todayCount;
        BigDecimal monthAmount;
        long monthCount;
        if (filter == null) {
            totalAmount = nz(paybillCollectionRepository.sumAmount());
            totalCount = paybillCollectionRepository.count();
            todayAmount = nz(paybillCollectionRepository.sumAmountSince(startOfToday));
            todayCount = paybillCollectionRepository.countByCreatedAtGreaterThanEqual(startOfToday);
            monthAmount = nz(paybillCollectionRepository.sumAmountSince(startOfMonth));
            monthCount = paybillCollectionRepository.countByCreatedAtGreaterThanEqual(startOfMonth);
        } else {
            totalAmount = nz(paybillCollectionRepository.sumAmountByBillRef(filter));
            totalCount = paybillCollectionRepository.countByBillRefIgnoreCase(filter);
            todayAmount = nz(paybillCollectionRepository.sumAmountByBillRefSince(filter, startOfToday));
            todayCount = paybillCollectionRepository.countByBillRefIgnoreCaseAndCreatedAtGreaterThanEqual(filter, startOfToday);
            monthAmount = nz(paybillCollectionRepository.sumAmountByBillRefSince(filter, startOfMonth));
            monthCount = paybillCollectionRepository.countByBillRefIgnoreCaseAndCreatedAtGreaterThanEqual(filter, startOfMonth);
        }

        Map<String, AccountStat> byRef = new LinkedHashMap<>();
        for (String account : collectionAccounts()) {
            byRef.put(account, AccountStat.builder()
                    .billRef(account)
                    .count(0)
                    .amount(BigDecimal.ZERO.setScale(4))
                    .build());
        }
        for (Object[] row : paybillCollectionRepository.totalsByBillRef()) {
            String ref = String.valueOf(row[0]);
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            BigDecimal amount = row[2] instanceof BigDecimal bd ? bd : nz(null);
            byRef.put(ref, AccountStat.builder()
                    .billRef(ref)
                    .count(count)
                    .amount(amount)
                    .build());
        }

        return PaybillCollectionDashboardResponse.builder()
                .paybill(appProperties.getMpesa().getShortcode())
                .accounts(collectionAccounts())
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .todayAmount(todayAmount)
                .todayCount(todayCount)
                .monthAmount(monthAmount)
                .monthCount(monthCount)
                .byAccount(new ArrayList<>(byRef.values()))
                .recent(recent.map(this::toResponse))
                .build();
    }

    private List<String> collectionAccounts() {
        return smsSettingsService.collectionAccounts();
    }

    private PaybillCollectionResponse toResponse(PaybillCollection collection) {
        return PaybillCollectionResponse.builder()
                .id(collection.getId())
                .billRef(collection.getBillRef())
                .amount(collection.getAmount())
                .mpesaReceipt(collection.getMpesaReceipt())
                .phoneNumber(collection.getPhoneNumber())
                .mpesaTransactionDate(collection.getMpesaTransactionDate())
                .payerName(collection.getPayerName())
                .createdAt(collection.getCreatedAt())
                .build();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4) : value;
    }

    private static String joinName(String firstName, String middleName, String lastName) {
        String joined = String.join(" ",
                firstName == null ? "" : firstName.trim(),
                middleName == null ? "" : middleName.trim(),
                lastName == null ? "" : lastName.trim())
                .replaceAll("\\s+", " ")
                .trim();
        return joined.isEmpty() ? null : joined;
    }
}
