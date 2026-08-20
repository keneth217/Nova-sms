package com.novastack.sms.service;

import com.novastack.sms.domain.entity.MpesaC2bInbound;
import com.novastack.sms.domain.repository.MpesaC2bInboundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class C2bInboundService {

    private static final int PAYLOAD_MAX = 4000;
    private static final int PHONE_MAX = 64;

    private final MpesaC2bInboundRepository inboundRepository;

    /**
     * Commits independently of wallet credit so a later save failure still leaves
     * TransID + BillRefNumber + amount for receipt recovery.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MpesaC2bInbound capture(
            String receipt,
            String billRef,
            BigDecimal amount,
            String phoneNumber,
            String transactionDate,
            String payload) {
        if (receipt == null || receipt.isBlank() || amount == null) {
            return null;
        }
        String normalized = receipt.trim().toUpperCase(Locale.ROOT);
        Optional<MpesaC2bInbound> existing = inboundRepository.findByMpesaReceiptIgnoreCase(normalized);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return inboundRepository.save(MpesaC2bInbound.builder()
                    .mpesaReceipt(normalized)
                    .billRef(blankToNull(billRef))
                    .amount(amount)
                    .phoneNumber(clip(phoneNumber, PHONE_MAX))
                    .mpesaTransactionDate(blankToNull(transactionDate))
                    .payload(clip(payload, PAYLOAD_MAX))
                    .credited(false)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return inboundRepository.findByMpesaReceiptIgnoreCase(normalized).orElse(null);
        }
    }

    @Transactional(readOnly = true)
    public Optional<MpesaC2bInbound> findByReceipt(String receipt) {
        if (receipt == null || receipt.isBlank()) {
            return Optional.empty();
        }
        return inboundRepository.findByMpesaReceiptIgnoreCase(receipt.trim());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCredited(String receipt) {
        if (receipt == null || receipt.isBlank()) {
            return;
        }
        inboundRepository.findByMpesaReceiptIgnoreCase(receipt.trim()).ifPresent(row -> {
            if (!row.isCredited()) {
                row.setCredited(true);
                inboundRepository.save(row);
            }
        });
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
