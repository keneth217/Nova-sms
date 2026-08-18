package com.novastack.sms.scheduler;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.provider.TalkSasaSmsProvider;
import com.novastack.sms.service.SmsStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "novastack.sms.status-sync.enabled", havingValue = "true", matchIfMissing = true)
public class SmsStatusSyncScheduler {

    private static final List<MessageStatus> IN_FLIGHT = List.of(
            MessageStatus.PENDING,
            MessageStatus.QUEUED,
            MessageStatus.PROCESSING,
            MessageStatus.ACCEPTED,
            MessageStatus.SENT
    );

    private final SmsMessageRepository smsMessageRepository;
    private final SmsStatusService smsStatusService;
    private final AppProperties appProperties;

    @Scheduled(cron = "${novastack.sms.status-sync.cron:0 */5 * * * *}")
    public void syncPendingStatuses() {
        int limit = Math.max(1, appProperties.getSms().getStatusSync().getBatchSize());
        Instant sentBefore = Instant.now().minus(30, ChronoUnit.SECONDS);
        List<SmsMessage> pending = smsMessageRepository.findForProviderStatusSync(
                TalkSasaSmsProvider.PROVIDER_NAME,
                IN_FLIGHT,
                sentBefore,
                PageRequest.of(0, limit)
        );
        if (pending.isEmpty()) {
            return;
        }

        log.info("Syncing TalkSasa status for {} in-flight messages", pending.size());
        for (SmsMessage message : pending) {
            try {
                smsStatusService.syncMessage(message);
            } catch (Exception ex) {
                log.warn("Status sync failed for {}: {}", message.getId(), ex.getMessage());
            }
        }
    }
}
