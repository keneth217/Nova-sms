package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.ProviderRequestLog;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.ProviderRequestLogRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsDeliveryService {

    private final SmsMessageRepository smsMessageRepository;
    private final ProviderRequestLogRepository providerRequestLogRepository;
    private final SmsProviderFactory smsProviderFactory;
    private final WalletService walletService;
    private final AppProperties appProperties;

    @Transactional
    public void processQueuedMessage(UUID messageId) {
        SmsMessage message = smsMessageRepository.findByIdWithOrganization(messageId).orElse(null);
        if (message == null) {
            log.warn("SMS message not found: {}", messageId);
            return;
        }

        if (message.getStatus() != MessageStatus.PENDING) {
            log.debug("Skipping message {} with status {}", messageId, message.getStatus());
            return;
        }

        int maxRetries = appProperties.getSms().getMaxRetries();
        SmsProvider provider = smsProviderFactory.getDefaultProvider();

        while (true) {
            var request = smsProviderFactory.buildRequest(
                    message.getOrganization(),
                    message.getRecipient(),
                    message.getContent(),
                    message.getSenderId()
            );

            SmsProvider.SmsProviderResult result = provider.send(request);
            saveProviderLog(message.getId(), provider.getName(), result);

            if (result.success()) {
                markSent(message, result.providerMessageId());
                return;
            }

            message.setRetryCount(message.getRetryCount() + 1);
            if (message.getRetryCount() < maxRetries) {
                smsMessageRepository.save(message);
                log.warn("Retrying SMS {} attempt={}", messageId, message.getRetryCount());
                continue;
            }

            markFailedAndRefund(message, result.errorMessage());
            return;
        }
    }

    /**
     * Sends all PENDING messages in a batch with one Africa's Talking bulk request
     * ({@code to} = comma-separated recipients, {@code bulkSMSMode=1}, {@code enqueue=1}).
     */
    @Transactional
    public void processQueuedBatch(UUID batchId) {
        List<SmsMessage> messages = smsMessageRepository
                .findByBatchIdAndStatusWithOrganization(batchId, MessageStatus.PENDING);
        if (messages.isEmpty()) {
            return;
        }

        SmsMessage first = messages.getFirst();
        SmsProvider provider = smsProviderFactory.getDefaultProvider();
        List<String> recipients = messages.stream().map(SmsMessage::getRecipient).toList();

        Map<String, SmsProvider.SmsProviderResult> results = provider.sendBulk(
                smsProviderFactory.buildBulkRequest(
                        first.getOrganization(),
                        recipients,
                        first.getContent(),
                        first.getSenderId()
                )
        );

        SmsProvider.SmsProviderResult sample = results.values().stream().findFirst().orElse(null);
        if (sample != null) {
            saveProviderLog(first.getId(), provider.getName(), sample);
        }

        for (SmsMessage message : messages) {
            SmsProvider.SmsProviderResult result = results.get(normalizePhoneKey(message.getRecipient()));
            if (result == null) {
                markFailedAndRefund(message, "No Africa's Talking result for recipient");
                continue;
            }
            if (result.success()) {
                markSent(message, result.providerMessageId());
            } else {
                markFailedAndRefund(message, result.errorMessage());
            }
        }
    }

    /** Provider accepted the message — stay PENDING until DLR sets DELIVERED/FAILED. */
    private void markSent(SmsMessage message, String providerMessageId) {
        if (message.getStatus() == MessageStatus.DELIVERED || message.getStatus() == MessageStatus.FAILED) {
            return;
        }
        message.setStatus(MessageStatus.PENDING);
        message.setProviderMessageId(providerMessageId);
        message.setSentAt(Instant.now());
        message.setFailureReason(null);
        smsMessageRepository.save(message);
    }

    private void markFailedAndRefund(SmsMessage message, String errorMessage) {
        message.setStatus(MessageStatus.FAILED);
        message.setFailureReason(errorMessage);
        smsMessageRepository.save(message);
        walletService.refund(
                message.getOrganization().getId(),
                message.getCost(),
                "REFUND-" + message.getId(),
                "Refund for failed SMS " + message.getId()
        );
    }

    private void saveProviderLog(UUID messageId, String providerName, SmsProvider.SmsProviderResult result) {
        providerRequestLogRepository.save(ProviderRequestLog.builder()
                .smsMessageId(messageId)
                .provider(providerName)
                .requestPayload(result.rawRequest())
                .responsePayload(result.rawResponse())
                .httpStatus(result.httpStatus())
                .success(result.success())
                .build());
    }

    private String normalizePhoneKey(String phone) {
        if (phone == null) {
            return "";
        }
        String cleaned = phone.trim().replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        return cleaned;
    }
}
