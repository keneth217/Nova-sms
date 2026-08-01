package com.novastack.sms.service;

import com.novastack.sms.domain.entity.DataBundleTransaction;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.BundleStatus;
import com.novastack.sms.domain.repository.DataBundleTransactionRepository;
import com.novastack.sms.dto.response.DataBundleMetricsResponse;
import com.novastack.sms.dto.response.DataBundleTransactionResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates and queries data-bundle purchase transactions for audit and reconciliation.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final DateTimeFormatter REF_DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final AtomicInteger REF_SEQ = new AtomicInteger(1);

    private final DataBundleTransactionRepository transactionRepository;
    private final WalletService walletService;

    @Transactional
    public DataBundleTransaction createPending(
            Organization org,
            String reference,
            String phone,
            String offerId,
            String offerName,
            String category,
            BigDecimal amount,
            boolean walletDebited) {
        return transactionRepository.save(DataBundleTransaction.builder()
                .organization(org)
                .reference(reference)
                .phoneNumber(phone)
                .offerId(offerId)
                .offerName(offerName)
                .category(category)
                .amount(amount)
                .status(BundleStatus.PENDING)
                .walletDebited(walletDebited)
                .build());
    }

    @Transactional(readOnly = true)
    public DataBundleTransaction requireByReference(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ApiException("Transaction not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public DataBundleTransaction requireForOrganization(UUID organizationId, String reference) {
        return transactionRepository
                .findByOrganizationIdAndReference(organizationId, reference)
                .orElseThrow(() -> new ApiException("Transaction not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<DataBundleTransactionResponse> history(
            UUID organizationId,
            BundleStatus status,
            String phone,
            Instant from,
            Instant to,
            Pageable pageable) {
        return transactionRepository
                .search(organizationId, status, blankToNull(phone), from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DataBundleMetricsResponse metrics(UUID organizationId) {
        long successful = transactionRepository.countByOrganizationIdAndStatus(organizationId, BundleStatus.SUCCESS);
        long failed = transactionRepository.countByOrganizationIdAndStatus(organizationId, BundleStatus.FAILED);
        long pending = transactionRepository.countByOrganizationIdAndStatus(organizationId, BundleStatus.PENDING);
        BigDecimal revenue = transactionRepository.sumAmountByOrganizationIdAndStatus(
                organizationId, BundleStatus.SUCCESS);
        return DataBundleMetricsResponse.builder()
                .totalSold(successful)
                .successful(successful)
                .failed(failed)
                .pending(pending)
                .revenue(revenue == null ? BigDecimal.ZERO : revenue)
                .build();
    }

    @Transactional
    public void failAndRefund(DataBundleTransaction tx, String reason) {
        tx.setStatus(BundleStatus.FAILED);
        tx.setFailureReason(truncate(reason, 500));
        transactionRepository.save(tx);
        if (tx.isWalletDebited()) {
            walletService.refund(
                    tx.getOrganization().getId(),
                    tx.getAmount(),
                    "BUNDLE-REFUND-" + tx.getReference(),
                    "Refund for failed data bundle " + tx.getReference());
            tx.setWalletDebited(false);
            transactionRepository.save(tx);
        }
    }

    public String resolveReference(UUID organizationId, String requested) {
        if (requested != null && !requested.isBlank()) {
            String ref = requested.trim().toUpperCase();
            if (transactionRepository.existsByOrganizationIdAndReference(organizationId, ref)) {
                throw new ApiException("Duplicate reference: " + ref, HttpStatus.CONFLICT);
            }
            return ref;
        }
        String generated;
        do {
            generated = "NP-" + LocalDate.now().format(REF_DAY) + "-"
                    + String.format("%04d", REF_SEQ.getAndIncrement() % 10_000);
        } while (transactionRepository.existsByOrganizationIdAndReference(organizationId, generated));
        return generated;
    }

    public DataBundleTransactionResponse toResponse(DataBundleTransaction tx) {
        return DataBundleTransactionResponse.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .phoneNumber(tx.getPhoneNumber())
                .offerId(tx.getOfferId())
                .offerName(tx.getOfferName())
                .category(tx.getCategory())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .checkoutRequestId(tx.getCheckoutRequestId())
                .responseCode(tx.getResponseCode())
                .responseDescription(tx.getResponseDescription())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
