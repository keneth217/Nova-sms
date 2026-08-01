package com.novastack.sms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.DataBundleCallbackLog;
import com.novastack.sms.domain.entity.DataBundleTransaction;
import com.novastack.sms.domain.enums.BundleStatus;
import com.novastack.sms.domain.repository.DataBundleCallbackLogRepository;
import com.novastack.sms.domain.repository.DataBundleTransactionRepository;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataBundleCallbackService {

    private final DataBundleCallbackLogRepository callbackLogRepository;
    private final DataBundleTransactionRepository transactionRepository;
    private final WalletService walletService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> handleCallback(String payload, String callbackToken) {
        verifyCallbackToken(callbackToken);

        DataBundleCallbackLog logEntry = callbackLogRepository.save(DataBundleCallbackLog.builder()
                .payload(payload == null ? "{}" : payload)
                .processed(false)
                .build());

        try {
            JsonNode root = objectMapper.readTree(payload == null ? "{}" : payload);
            String reference = firstText(root, "reference", "RequestID", "requestId", "OriginatorConversationID", "BillRefNumber");
            String status = firstText(root, "status", "Status", "TransactionStatus", "ResultDesc");
            String code = firstText(root, "ResponseCode", "responseCode", "ResultCode");
            String description = firstText(root, "ResponseDescription", "responseDescription", "ResultDesc", "message");

            logEntry.setReference(reference);
            if (reference == null || reference.isBlank()) {
                logEntry.setProcessError("Missing reference in callback");
                callbackLogRepository.save(logEntry);
                return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
            }

            DataBundleTransaction tx = transactionRepository.findByReference(reference.trim()).orElse(null);
            if (tx == null) {
                logEntry.setProcessError("Unknown reference " + reference);
                callbackLogRepository.save(logEntry);
                return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
            }

            applyRemoteStatus(tx, status, code, description);
            logEntry.setProcessed(true);
            callbackLogRepository.save(logEntry);
            return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
        } catch (Exception ex) {
            log.error("Data bundle callback processing failed: {}", ex.getMessage(), ex);
            logEntry.setProcessError(truncate(ex.getMessage(), 500));
            callbackLogRepository.save(logEntry);
            return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
        }
    }

    @Transactional
    public void applyRemoteStatus(
            DataBundleTransaction tx,
            String statusText,
            String responseCode,
            String responseDescription) {
        if (tx.getStatus() == BundleStatus.SUCCESS || tx.getStatus() == BundleStatus.FAILED
                || tx.getStatus() == BundleStatus.CANCELLED) {
            return;
        }

        BundleStatus mapped = mapStatus(statusText, responseCode);
        tx.setResponseCode(responseCode);
        tx.setResponseDescription(responseDescription);

        if (mapped == BundleStatus.SUCCESS) {
            tx.setStatus(BundleStatus.SUCCESS);
            tx.setFailureReason(null);
            transactionRepository.save(tx);
            return;
        }

        if (mapped == BundleStatus.FAILED || mapped == BundleStatus.CANCELLED) {
            tx.setStatus(mapped);
            tx.setFailureReason(truncate(
                    responseDescription == null ? statusText : responseDescription, 500));
            transactionRepository.save(tx);
            if (tx.isWalletDebited()) {
                walletService.refund(
                        tx.getOrganization().getId(),
                        tx.getAmount(),
                        "BUNDLE-REFUND-" + tx.getReference(),
                        "Refund for " + mapped.name().toLowerCase(Locale.ROOT)
                                + " data bundle " + tx.getReference());
                tx.setWalletDebited(false);
                transactionRepository.save(tx);
            }
        }
    }

    private void verifyCallbackToken(String callbackToken) {
        String expected = appProperties.getDataBundles().getCallbackToken();
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (callbackToken == null || !expected.equals(callbackToken)) {
            throw new ApiException("Invalid callback token", HttpStatus.UNAUTHORIZED);
        }
    }

    private BundleStatus mapStatus(String statusText, String responseCode) {
        String hay = ((statusText == null ? "" : statusText) + " " + (responseCode == null ? "" : responseCode))
                .toLowerCase(Locale.ROOT);
        if (hay.contains("success") || hay.contains("completed") || responseCodeEquals(responseCode, "0", "00", "200")) {
            return BundleStatus.SUCCESS;
        }
        if (hay.contains("cancel")) {
            return BundleStatus.CANCELLED;
        }
        if (hay.contains("fail") || hay.contains("error") || hay.contains("reject") || hay.contains("insufficient")) {
            return BundleStatus.FAILED;
        }
        if (responseCodeEquals(responseCode, "1", "1032", "1037")) {
            return BundleStatus.FAILED;
        }
        return BundleStatus.PENDING;
    }

    private static boolean responseCodeEquals(String code, String... ok) {
        if (code == null) {
            return false;
        }
        for (String candidate : ok) {
            if (code.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String firstText(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.path(key);
            if (!node.isMissingNode() && !node.isNull()) {
                String text = node.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
            // nested Body.stkCallback style
            JsonNode nested = root.path("Body").path(key);
            if (!nested.isMissingNode() && !nested.isNull()) {
                String text = nested.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
