package com.novastack.sms.service;

import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.dto.response.TalkSasaAccountResponse;
import com.novastack.sms.provider.TalkSasaProfileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderUnitsAlertService {

    private final TalkSasaProfileClient talkSasaProfileClient;
    private final SmsSettingsService smsSettingsService;
    private final OrgNotificationService orgNotificationService;
    private final WalletRepository walletRepository;

    @Transactional
    public void checkAndAlert() {
        if (!smsSettingsService.isEnabled()) {
            return;
        }
        TalkSasaAccountResponse account = talkSasaProfileClient.getAccount();
        if (!account.isConfigured() || !account.isReachable()
                || account.getBalance() == null
                || account.getBalance().getRemainingUnits() == null) {
            log.warn("Skipping TalkSasa units alert — account unreachable or remaining units missing");
            return;
        }
        BigDecimal remaining = account.getBalance().getRemainingUnits();
        BigDecimal threshold = smsSettingsService.lowBalanceThreshold();
        boolean lowBelow = threshold != null && remaining.compareTo(threshold) <= 0;
        boolean notifyLow = lowBelow && !smsSettingsService.talksasaLowAlerted();

        BigDecimal wallets = walletRepository.sumAllBalances();
        boolean exposed = wallets.compareTo(remaining) > 0;
        boolean notifyExposure = exposed && !smsSettingsService.talksasaExposureAlerted();

        smsSettingsService.recordTalksasaAlertState(remaining, lowBelow, exposed);
        if (notifyLow) {
            orgNotificationService.notifyProviderLowUnits(remaining, threshold);
        }
        if (notifyExposure) {
            orgNotificationService.notifyWalletExposure(wallets, remaining);
        }
    }
}
