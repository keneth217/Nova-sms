package com.novastack.sms.service;

import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.dto.response.TalkSasaAccountResponse;
import com.novastack.sms.provider.TalkSasaProfileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderUnitsAlertServiceTest {

    @Mock
    private TalkSasaProfileClient talkSasaProfileClient;
    @Mock
    private SmsSettingsService smsSettingsService;
    @Mock
    private OrgNotificationService orgNotificationService;
    @Mock
    private WalletRepository walletRepository;

    private ProviderUnitsAlertService service;

    @BeforeEach
    void setUp() {
        service = new ProviderUnitsAlertService(
                talkSasaProfileClient, smsSettingsService, orgNotificationService, walletRepository);
        lenient().when(smsSettingsService.isEnabled()).thenReturn(true);
        lenient().when(smsSettingsService.lowBalanceThreshold()).thenReturn(new BigDecimal("50.00"));
        lenient().when(smsSettingsService.talksasaLowAlerted()).thenReturn(false);
        lenient().when(smsSettingsService.talksasaExposureAlerted()).thenReturn(false);
        lenient().when(walletRepository.sumAllBalances()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void alertsOnceWhenTalkSasaUnitsFallToThreshold() {
        when(talkSasaProfileClient.getAccount()).thenReturn(account(new BigDecimal("40.00")));

        service.checkAndAlert();

        verify(smsSettingsService).recordTalksasaAlertState(new BigDecimal("40.00"), true, false);
        verify(orgNotificationService).notifyProviderLowUnits(
                new BigDecimal("40.00"), new BigDecimal("50.00"));
        verify(orgNotificationService, never()).notifyWalletExposure(any(), any());
    }

    @Test
    void doesNotResendWhileStillBelowThreshold() {
        when(talkSasaProfileClient.getAccount()).thenReturn(account(new BigDecimal("20.00")));
        when(smsSettingsService.talksasaLowAlerted()).thenReturn(true);

        service.checkAndAlert();

        verify(smsSettingsService).recordTalksasaAlertState(new BigDecimal("20.00"), true, false);
        verify(orgNotificationService, never()).notifyProviderLowUnits(any(), any());
    }

    @Test
    void clearsAlertWhenTalkSasaUnitsRecover() {
        when(talkSasaProfileClient.getAccount()).thenReturn(account(new BigDecimal("80.00")));

        service.checkAndAlert();

        verify(smsSettingsService).recordTalksasaAlertState(new BigDecimal("80.00"), false, false);
        verify(orgNotificationService, never()).notifyProviderLowUnits(any(), any());
    }

    @Test
    void alertsWhenOrgWalletsExceedTalkSasaUnits() {
        when(talkSasaProfileClient.getAccount()).thenReturn(account(new BigDecimal("100.00")));
        when(walletRepository.sumAllBalances()).thenReturn(new BigDecimal("250.00"));

        service.checkAndAlert();

        verify(smsSettingsService).recordTalksasaAlertState(new BigDecimal("100.00"), false, true);
        verify(orgNotificationService).notifyWalletExposure(
                new BigDecimal("250.00"), new BigDecimal("100.00"));
        verify(orgNotificationService, never()).notifyProviderLowUnits(any(), any());
    }

    @Test
    void doesNotResendWalletExposureWhileStillOverUnits() {
        when(talkSasaProfileClient.getAccount()).thenReturn(account(new BigDecimal("90.00")));
        when(walletRepository.sumAllBalances()).thenReturn(new BigDecimal("200.00"));
        when(smsSettingsService.talksasaExposureAlerted()).thenReturn(true);

        service.checkAndAlert();

        verify(smsSettingsService).recordTalksasaAlertState(new BigDecimal("90.00"), false, true);
        verify(orgNotificationService, never()).notifyWalletExposure(any(), any());
    }

    @Test
    void skipsUnreachableTalkSasa() {
        when(talkSasaProfileClient.getAccount()).thenReturn(TalkSasaAccountResponse.builder()
                .configured(true)
                .reachable(false)
                .build());

        service.checkAndAlert();

        verify(smsSettingsService, never()).recordTalksasaAlertState(any(), eq(true), eq(true));
        verify(orgNotificationService, never()).notifyProviderLowUnits(any(), any());
        verify(orgNotificationService, never()).notifyWalletExposure(any(), any());
    }

    private static TalkSasaAccountResponse account(BigDecimal remaining) {
        return TalkSasaAccountResponse.builder()
                .configured(true)
                .reachable(true)
                .balance(TalkSasaAccountResponse.Balance.builder()
                        .remainingUnits(remaining)
                        .build())
                .build();
    }
}
