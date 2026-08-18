package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.ProviderRequestLog;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.BillingStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.ProviderRequestLogRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.provider.ProviderErrorMessages;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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

    public String currentProviderName() {
        return smsProviderFactory.getDefaultProvider().getName();
    }

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

        if (message.getProviderMessageId() != null && !message.getProviderMessageId().isBlank()) {
            log.info("Skipping resend for {} because providerMessageId is already set", messageId);
            return;
        }

        int maxRetries = Math.max(1, appProperties.getSms().getMaxRetries());
        SmsProvider provider = providerFor(message);
        message.setProvider(provider.getName());

        int attempts = 0;
        while (true) {
            attempts++;
            var request = smsProviderFactory.buildRequest(
                    message.getOrganization(),
                    message.getRecipient(),
                    message.getContent(),
                    message.getSenderId(),
                    talkSasaType(message)
            );

            SmsProvider.SmsProviderResult result = provider.send(request);
            saveProviderLog(message.getId(), provider.getName(), result);

            if (result.success()) {
                markAccepted(message, result);
                return;
            }

            message.setRetryCount(message.getRetryCount() + 1);
            if (result.retryable() && attempts < maxRetries) {
                smsMessageRepository.save(message);
                log.warn("Retrying SMS {} attempt={} retryable provider error", messageId, attempts);
                backoff(attempts);
                continue;
            }

            markFailedAndRefund(message, result);
            return;
        }
    }

    @Transactional
    public void processQueuedBatch(UUID batchId) {
        List<SmsMessage> messages = smsMessageRepository
                .findByBatchIdAndStatusWithOrganization(batchId, MessageStatus.PENDING);
        if (messages.isEmpty()) {
            return;
        }

        SmsProvider provider = providerFor(messages.getFirst());
        int batchSize = Math.max(1, appProperties.getSms().getBatchSize());

        for (int from = 0; from < messages.size(); from += batchSize) {
            int to = Math.min(from + batchSize, messages.size());
            processChunk(provider, messages.subList(from, to));
        }
    }

    private void processChunk(SmsProvider provider, List<SmsMessage> messages) {
        List<SmsMessage> pending = new ArrayList<>();
        for (SmsMessage message : messages) {
            message.setProvider(provider.getName());
            if (message.getProviderMessageId() != null && !message.getProviderMessageId().isBlank()) {
                continue;
            }
            pending.add(message);
        }
        if (pending.isEmpty()) {
            return;
        }

        SmsMessage first = pending.getFirst();
        List<String> recipients = pending.stream().map(SmsMessage::getRecipient).toList();

        Map<String, SmsProvider.SmsProviderResult> results = sendChunkWithRetry(provider, first, recipients);

        SmsProvider.SmsProviderResult sample = results.values().stream().findFirst().orElse(null);
        if (sample != null) {
            saveProviderLog(first.getId(), provider.getName(), sample);
        }

        for (SmsMessage message : pending) {
            SmsProvider.SmsProviderResult result = results.get(PhoneNormalizer.lookupKey(message.getRecipient()));
            if (result == null) {
                result = results.values().stream()
                        .filter(SmsProvider.SmsProviderResult::success)
                        .findFirst()
                        .orElse(null);
            }
            if (result == null) {
                markFailedAndRefund(message, SmsProvider.SmsProviderResult.failure(
                        null, null, null, ProviderErrorMessages.MALFORMED, false));
                continue;
            }
            if (result.success()) {
                markAccepted(message, result);
            } else {
                markFailedAndRefund(message, result);
            }
        }
    }

    private Map<String, SmsProvider.SmsProviderResult> sendChunkWithRetry(
            SmsProvider provider,
            SmsMessage sample,
            List<String> recipients) {
        int maxRetries = Math.max(1, appProperties.getSms().getMaxRetries());
        Map<String, SmsProvider.SmsProviderResult> results = Map.of();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            results = provider.sendBulk(smsProviderFactory.buildBulkRequest(
                    sample.getOrganization(),
                    recipients,
                    sample.getContent(),
                    sample.getSenderId(),
                    talkSasaType(sample)
            ));
            boolean anyRetryable = results.values().stream().anyMatch(r -> !r.success() && r.retryable());
            boolean allFailedRetryable = !results.isEmpty()
                    && results.values().stream().allMatch(r -> !r.success() && r.retryable());
            if (allFailedRetryable && attempt < maxRetries) {
                log.warn("Retrying bulk SMS batch {} attempt={}", sample.getBatchId(), attempt);
                backoff(attempt);
                continue;
            }
            if (anyRetryable && attempt < maxRetries && results.values().stream().noneMatch(SmsProvider.SmsProviderResult::success)) {
                backoff(attempt);
                continue;
            }
            break;
        }
        return results;
    }

    private void markAccepted(SmsMessage message, SmsProvider.SmsProviderResult result) {
        if (message.getStatus() == MessageStatus.DELIVERED || message.getStatus().isBillableFailure()) {
            return;
        }
        MessageStatus mapped = result.mappedStatus() != null ? result.mappedStatus() : MessageStatus.ACCEPTED;
        if (mapped == MessageStatus.PENDING) {
            mapped = MessageStatus.ACCEPTED;
        }
        message.setStatus(mapped);
        message.setProviderMessageId(result.providerMessageId());
        message.setSentAt(Instant.now());
        message.setFailureReason(null);
        if (mapped == MessageStatus.DELIVERED) {
            message.setDeliveredAt(Instant.now());
        }
        smsMessageRepository.save(message);
        log.info("SMS {} recipient={} status={} providerMessageId={} providerHttpStatus={} providerBody={}",
                message.getId(),
                message.getRecipient(),
                mapped,
                result.providerMessageId(),
                result.httpStatus(),
                logBody(result.rawResponse()));
    }

    private void markFailedAndRefund(SmsMessage message, SmsProvider.SmsProviderResult result) {
        MessageStatus status = result.mappedStatus() != null && result.mappedStatus().isBillableFailure()
                ? result.mappedStatus()
                : MessageStatus.FAILED;
        String error = result.errorMessage() != null ? result.errorMessage() : ProviderErrorMessages.UNAVAILABLE;
        log.warn("SMS {} recipient={} status={} failureReason={} providerHttpStatus={} providerBody={}",
                message.getId(),
                message.getRecipient(),
                status,
                error,
                result.httpStatus(),
                logBody(result.rawResponse()));
        markFailedAndRefund(message, status, error);
    }

    private void markFailedAndRefund(SmsMessage message, MessageStatus status, String errorMessage) {
        message.setStatus(status);
        message.setFailureReason(errorMessage);
        if (message.getBillingStatus() == BillingStatus.REFUNDED) {
            smsMessageRepository.save(message);
            return;
        }
        walletService.refund(
                message.getOrganization().getId(),
                message.getCost(),
                "REFUND-" + message.getId(),
                "Refund for failed " + channelLabel(message) + " " + message.getId()
        );
        message.setBillingStatus(BillingStatus.REFUNDED);
        smsMessageRepository.save(message);
    }

    private void saveProviderLog(UUID messageId, String providerName, SmsProvider.SmsProviderResult result) {
        providerRequestLogRepository.save(ProviderRequestLog.builder()
                .smsMessageId(messageId)
                .provider(providerName)
                .requestPayload(redact(result.rawRequest()))
                .responsePayload(redact(result.rawResponse()))
                .httpStatus(result.httpStatus())
                .success(result.success())
                .build());
    }

    private SmsProvider providerFor(SmsMessage message) {
        if (channelOf(message) == MessageChannel.WHATSAPP) {
            return smsProviderFactory.getProvider(SmsProviderFactory.TALKSASA);
        }
        return smsProviderFactory.getDefaultProvider();
    }

    private static MessageChannel channelOf(SmsMessage message) {
        return message.getChannel() == null ? MessageChannel.SMS : message.getChannel();
    }

    private static String talkSasaType(SmsMessage message) {
        return channelOf(message).talkSasaType();
    }

    private static String channelLabel(SmsMessage message) {
        return channelOf(message).displayName();
    }

    private static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value
                .replaceAll("(?i)(Bearer\\s+)\\S+", "$1***")
                .replaceAll("(?i)(api[_-]?token\"?\\s*[:=]\\s*\")[^\"]+", "$1***");
    }

    private static String logBody(String value) {
        String redacted = redact(value);
        if (redacted == null || redacted.isBlank()) {
            return "(empty)";
        }
        return redacted.length() > 8_000 ? redacted.substring(0, 8_000) + "…" : redacted;
    }

    private void backoff(int attempt) {
        long delayMs = Math.min(2_000L, 200L * (1L << Math.max(0, attempt - 1)));
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
