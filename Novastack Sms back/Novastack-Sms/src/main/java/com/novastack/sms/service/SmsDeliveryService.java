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

        if (message.getStatus() != MessageStatus.QUEUED) {
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

            providerRequestLogRepository.save(ProviderRequestLog.builder()
                    .smsMessageId(message.getId())
                    .provider(provider.getName())
                    .requestPayload(result.rawRequest())
                    .responsePayload(result.rawResponse())
                    .httpStatus(result.httpStatus())
                    .success(result.success())
                    .build());

            if (result.success()) {
                message.setStatus(MessageStatus.SENT);
                message.setProviderMessageId(result.providerMessageId());
                message.setSentAt(Instant.now());
                message.setFailureReason(null);
                smsMessageRepository.save(message);
                return;
            }

            message.setRetryCount(message.getRetryCount() + 1);
            if (message.getRetryCount() < maxRetries) {
                smsMessageRepository.save(message);
                log.warn("Retrying SMS {} attempt={}", messageId, message.getRetryCount());
                continue;
            }

            message.setStatus(MessageStatus.FAILED);
            message.setFailureReason(result.errorMessage());
            smsMessageRepository.save(message);

            walletService.refund(
                    message.getOrganization().getId(),
                    message.getCost(),
                    "REFUND-" + message.getId(),
                    "Refund for failed SMS " + message.getId()
            );
            return;
        }
    }
}
