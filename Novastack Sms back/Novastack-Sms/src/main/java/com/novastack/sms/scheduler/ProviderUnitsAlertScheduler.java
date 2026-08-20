package com.novastack.sms.scheduler;

import com.novastack.sms.service.ProviderUnitsAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "novastack.sms.provider-units-alert.enabled", havingValue = "true", matchIfMissing = true)
public class ProviderUnitsAlertScheduler {

    private final ProviderUnitsAlertService providerUnitsAlertService;

    @Scheduled(cron = "${novastack.sms.provider-units-alert.cron:0 */10 * * * *}")
    public void checkProviderUnits() {
        try {
            providerUnitsAlertService.checkAndAlert();
        } catch (Exception ex) {
            log.warn("TalkSasa units alert check failed: {}", ex.getMessage());
        }
    }
}
