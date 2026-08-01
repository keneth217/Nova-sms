package com.novastack.sms.scheduler;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.service.SmsDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledSmsDispatcher {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsDeliveryService smsDeliveryService;

    @Scheduled(fixedDelayString = "${novastack.scheduler.fixed-delay-ms:30000}")
    @Transactional
    public void dispatchDueMessages() {
        List<SmsMessage> due = smsMessageRepository
                .findDueScheduledMessages(MessageStatus.SCHEDULED, Instant.now());

        for (SmsMessage message : due) {
            message.setStatus(MessageStatus.PENDING);
            smsMessageRepository.save(message);
            smsDeliveryService.processQueuedMessage(message.getId());
            log.info("Dispatched scheduled SMS {}", message.getId());
        }
    }
}
