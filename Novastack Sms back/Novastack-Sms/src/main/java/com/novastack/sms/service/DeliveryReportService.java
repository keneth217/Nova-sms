package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsDeliveryReport;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsDeliveryReportRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryReportService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsDeliveryReportRepository deliveryReportRepository;

    @Transactional
    public void handleCallback(Map<String, String> payload) {
        String providerMessageId = firstNonBlank(payload, "id", "messageId");
        String statusText = firstNonBlank(payload, "status", "failureReason");
        String networkCode = payload.get("networkCode");
        String failureReason = payload.get("failureReason");

        log.info("DLR callback id={} status={}", providerMessageId, statusText);

        if (providerMessageId == null) {
            return;
        }

        SmsMessage message = smsMessageRepository.findByProviderMessageId(providerMessageId).orElse(null);
        if (message == null) {
            log.warn("No SMS found for provider message id {}", providerMessageId);
            return;
        }

        MessageStatus status = mapStatus(statusText);
        message.setStatus(status);
        if (status == MessageStatus.DELIVERED) {
            message.setDeliveredAt(Instant.now());
        } else if (status == MessageStatus.FAILED) {
            message.setFailureReason(failureReason != null ? failureReason : statusText);
        }
        smsMessageRepository.save(message);

        deliveryReportRepository.save(SmsDeliveryReport.builder()
                .smsMessage(message)
                .providerMessageId(providerMessageId)
                .networkCode(networkCode)
                .failureReason(failureReason)
                .status(status)
                .rawPayload(payload.toString())
                .build());
    }

    private MessageStatus mapStatus(String statusText) {
        if (statusText == null) {
            return MessageStatus.SENT;
        }
        String normalized = statusText.trim().toLowerCase();
        if (normalized.contains("deliver") || normalized.equals("success")) {
            return MessageStatus.DELIVERED;
        }
        if (normalized.contains("fail") || normalized.contains("reject") || normalized.contains("undeliver")) {
            return MessageStatus.FAILED;
        }
        if (normalized.contains("sent") || normalized.contains("submitted")) {
            return MessageStatus.SENT;
        }
        return MessageStatus.SENT;
    }

    private String firstNonBlank(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
