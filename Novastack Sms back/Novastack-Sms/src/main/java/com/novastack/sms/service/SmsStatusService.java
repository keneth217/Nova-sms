package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import com.novastack.sms.provider.TalkSasaSmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsStatusService {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsProviderFactory smsProviderFactory;

    @Transactional
    public void syncMessage(SmsMessage message) {
        if (message == null || message.getStatus() == null || message.getStatus().isTerminal()) {
            return;
        }
        if (message.getProviderMessageId() == null || message.getProviderMessageId().isBlank()) {
            return;
        }

        SmsProvider provider = resolveProvider(message.getProvider());
        if (!provider.supportsStatusLookup()) {
            return;
        }

        SmsProvider.SmsStatusResult result = provider.getSmsStatus(message.getProviderMessageId());
        if (!result.success() || result.status() == null) {
            log.debug("Status sync skipped for {} reason={}", message.getId(), result.errorMessage());
            return;
        }
        if (!applyStatus(message, result)) {
            return;
        }
        smsMessageRepository.save(message);
    }

    private boolean applyStatus(SmsMessage message, SmsProvider.SmsStatusResult result) {
        MessageStatus mapped = result.status();
        String providerStatus = result.providerStatus();
        if (message.getStatus() == MessageStatus.DELIVERED && mapped != MessageStatus.DELIVERED) {
            return false;
        }
        if (message.getStatus().isBillableFailure() && mapped.isInFlight()) {
            return false;
        }
        if (mapped == MessageStatus.PENDING && message.getStatus() != MessageStatus.PENDING) {
            return false;
        }
        if (mapped == MessageStatus.ACCEPTED && message.getStatus() == MessageStatus.SENT) {
            return false;
        }

        MessageStatus oldStatus = message.getStatus();
        message.setStatus(mapped);
        if ((mapped == MessageStatus.SENT || mapped == MessageStatus.DELIVERED) && message.getSentAt() == null) {
            message.setSentAt(result.occurredAt() != null ? result.occurredAt() : Instant.now());
        }
        if (mapped == MessageStatus.DELIVERED) {
            message.setDeliveredAt(result.occurredAt() != null ? result.occurredAt() : Instant.now());
            message.setFailureReason(null);
        } else if (mapped.isBillableFailure()) {
            if (message.getFailureReason() == null || message.getFailureReason().isBlank()) {
                message.setFailureReason(providerStatus != null ? providerStatus : mapped.name());
            }
        }
        log.info("SMS status updated messageId={} oldStatus={} newStatus={}",
                message.getId(), oldStatus, mapped);
        return true;
    }

    private SmsProvider resolveProvider(String storedName) {
        if (storedName == null || storedName.isBlank() || TalkSasaSmsProvider.PROVIDER_NAME.equalsIgnoreCase(storedName)) {
            return smsProviderFactory.getProvider("talksasa");
        }
        return smsProviderFactory.getProvider(storedName);
    }
}
