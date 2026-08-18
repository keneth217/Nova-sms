package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsDeliveryReport;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsDeliveryReportRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.dto.request.AfricasTalkingDlrCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Handles Africa's Talking SMS notifications posted as
 * application/x-www-form-urlencoded to our callback URLs.
 *
 * @see <a href="https://developers.africastalking.com/docs/sms/notifications">AT SMS Notifications</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryReportService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsDeliveryReportRepository deliveryReportRepository;

    @Transactional
    public void handleDeliveryReport(Map<String, String> payload) {
        handleDeliveryReport(AfricasTalkingDlrCallback.from(payload));
    }

    @Transactional
    public void handleDeliveryReport(AfricasTalkingDlrCallback callback) {
        log.info("AT DLR mapped id={} status={} phone={} network={} failureReason={} retryCount={}",
                callback.getId(),
                callback.getStatus(),
                callback.getPhoneNumber(),
                callback.getNetworkCode(),
                callback.getFailureReason(),
                callback.getRetryCount());

        if (!callback.hasId()) {
            log.warn("Ignoring AT DLR without message id. callback={}", callback);
            return;
        }

        SmsMessage message = smsMessageRepository.findByProviderMessageId(callback.getId()).orElse(null);
        if (message == null) {
            log.warn("No SMS found for AT message id {}", callback.getId());
            return;
        }

        MessageStatus mapped = mapDeliveryStatus(callback.getStatus());
        applyStatusUpdate(message, mapped, callback.getStatus(), callback.getFailureReason());
        smsMessageRepository.save(message);

        String phoneNumber = blankToNull(callback.getPhoneNumber());
        if (phoneNumber == null) {
            phoneNumber = message.getRecipient();
        }

        SmsDeliveryReport report = new SmsDeliveryReport();
        report.setSmsMessage(message);
        report.setProviderMessageId(callback.getId());
        report.setProviderStatus(blankToNull(callback.getStatus()));
        report.setPhoneNumber(phoneNumber);
        report.setNetworkCode(blankToNull(callback.getNetworkCode()));
        report.setFailureReason(buildFailureDetail(callback.getStatus(), callback.getFailureReason()));
        report.setStatus(mapped);
        report.setRawPayload(callback.asMap().toString());

        SmsDeliveryReport saved = deliveryReportRepository.save(report);
        log.info(
                "Saved DLR id={} providerStatus={} phoneNumber={} networkCode={} mappedStatus={} failureReason={}",
                saved.getId(),
                saved.getProviderStatus(),
                saved.getPhoneNumber(),
                saved.getNetworkCode(),
                saved.getStatus(),
                saved.getFailureReason());
    }

    public void handleIncomingMessage(Map<String, String> payload) {
        log.info("AT incoming SMS from={} to={} text={} id={} network={}",
                firstNonBlank(payload, "from"),
                firstNonBlank(payload, "to"),
                firstNonBlank(payload, "text"),
                firstNonBlank(payload, "id"),
                firstNonBlank(payload, "networkCode"));
    }

    public void handleBulkOptOut(Map<String, String> payload) {
        log.info("AT bulk SMS opt-out senderId={} phoneNumber={}",
                firstNonBlank(payload, "senderId"),
                firstNonBlank(payload, "phoneNumber"));
    }

    public void handleSubscription(Map<String, String> payload) {
        log.info("AT subscription phone={} shortCode={} keyword={} updateType={}",
                firstNonBlank(payload, "phoneNumber"),
                firstNonBlank(payload, "shortCode"),
                firstNonBlank(payload, "keyword"),
                firstNonBlank(payload, "updateType"));
    }

    private void applyStatusUpdate(
            SmsMessage message,
            MessageStatus mapped,
            String statusText,
            String failureReason) {
        if (message.getStatus() == MessageStatus.DELIVERED && mapped != MessageStatus.DELIVERED) {
            return;
        }
        if (message.getStatus().isBillableFailure() && mapped.isInFlight()) {
            return;
        }

        message.setStatus(mapped);
        if (mapped == MessageStatus.DELIVERED) {
            message.setDeliveredAt(Instant.now());
            message.setFailureReason(null);
        } else if (mapped.isBillableFailure()) {
            message.setFailureReason(buildFailureDetail(statusText, failureReason));
        }
    }

    /**
     * Maps Africa's Talking delivery report statuses to customer-facing MessageStatus.
     * Final: Success → DELIVERED; Failed/Rejected/… → FAILED.
     * Intermediate (Sent/Submitted/Buffered) stay PENDING until a final DLR.
     */
    private MessageStatus mapDeliveryStatus(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return MessageStatus.PENDING;
        }

        return switch (statusText.trim().toLowerCase(Locale.ROOT)) {
            case "success" -> MessageStatus.DELIVERED;
            case "failed", "absentsubscriber", "expired" -> MessageStatus.FAILED;
            case "rejected" -> MessageStatus.REJECTED;
            case "sent" -> MessageStatus.SENT;
            case "submitted", "buffered" -> MessageStatus.ACCEPTED;
            default -> {
                String normalized = statusText.trim().toLowerCase(Locale.ROOT);
                if (normalized.contains("success") || normalized.contains("deliver")) {
                    yield MessageStatus.DELIVERED;
                }
                if (normalized.contains("reject")) {
                    yield MessageStatus.REJECTED;
                }
                if (normalized.contains("fail")
                        || normalized.contains("absent")
                        || normalized.contains("expir")) {
                    yield MessageStatus.FAILED;
                }
                if (normalized.contains("sent")) {
                    yield MessageStatus.SENT;
                }
                yield MessageStatus.PENDING;
            }
        };
    }

    private String buildFailureDetail(String statusText, String failureReason) {
        if (failureReason != null && !failureReason.isBlank()) {
            if (statusText != null && !statusText.isBlank()
                    && !statusText.equalsIgnoreCase(failureReason)) {
                return statusText + ": " + failureReason;
            }
            return failureReason;
        }
        return statusText;
    }

    private String firstNonBlank(Map<String, String> map, String... keys) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(key)
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()) {
                    return entry.getValue().trim();
                }
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
