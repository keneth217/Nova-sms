package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PlatformSmsSettings;
import com.novastack.sms.domain.repository.PlatformSmsSettingsRepository;
import com.novastack.sms.dto.request.UpdatePlatformSmsSettingsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsSettingsServiceTest {

    @Mock
    private PlatformSmsSettingsRepository repository;

    private SmsSettingsService service;
    private PlatformSmsSettings stored;

    @BeforeEach
    void setUp() {
        service = new SmsSettingsService(repository, new AppProperties());
        stored = new PlatformSmsSettings();
        stored.setId(PlatformSmsSettings.SINGLETON_ID);
        stored.setEnabled(true);
        stored.setLowBalanceThreshold(new BigDecimal("50.00"));
        stored.setPortalUrl("https://novasms.novastack.co.ke");
        stored.setTemplateWelcome("Welcome {name}");
        stored.setTemplateTopup("Topup {amount}");
        stored.setTemplateCollection("Collection {account}");
        stored.setTemplateLowBalance("Low {balance}");
        stored.setTemplatePlatformTopup("Owner {name} {account} {amount}");
        stored.setTemplateProviderLow("TalkSasa {units} {threshold}");
        stored.setTemplateProviderExposure("Wallets {wallets} units {units}");
        stored.setCollectionAccounts("SHEILA,KENETH");
        stored.setCollectionNotifyPhones("0711766223,0759728742");
        when(repository.findById(PlatformSmsSettings.SINGLETON_ID)).thenReturn(Optional.of(stored));
    }

    @Test
    void readsPersistedTemplates() {
        assertEquals("Welcome {name}", service.welcomeTemplate());
        assertEquals("Topup {amount}", service.topupTemplate());
        assertEquals("Collection {account}", service.collectionTemplate());
        assertEquals("Low {balance}", service.lowBalanceTemplate());
        assertEquals("Owner {name} {account} {amount}", service.platformTopupTemplate());
        assertEquals("TalkSasa {units} {threshold}", service.providerLowTemplate());
        assertEquals("Wallets {wallets} units {units}", service.providerExposureTemplate());
        assertEquals(List.of("SHEILA", "KENETH"), service.collectionAccounts());
        assertEquals(List.of("0711766223", "0759728742"), service.collectionNotifyPhones());
        assertTrue(service.isEnabled());
        assertEquals(0, new BigDecimal("50.00").compareTo(service.lowBalanceThreshold()));
    }

    @Test
    void updatePersistsAccountsPhonesAndThreshold() {
        when(repository.save(any(PlatformSmsSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdatePlatformSmsSettingsRequest request = new UpdatePlatformSmsSettingsRequest();
        request.setLowBalanceThreshold(new BigDecimal("75.00"));
        request.setCollectionAccounts(List.of("sheila", "Keneth", "MARY"));
        request.setCollectionNotifyPhones(List.of("0711766223", "0759728742"));

        PlatformSmsSettings saved = service.update(request);

        assertEquals(0, new BigDecimal("75.00").compareTo(saved.getLowBalanceThreshold()));
        assertEquals("SHEILA,KENETH,MARY", saved.getCollectionAccounts());
        assertEquals("0711766223,0759728742", saved.getCollectionNotifyPhones());
        verify(repository).save(stored);
    }

    @Test
    void updatePersistsEditedTemplate() {
        when(repository.save(any(PlatformSmsSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdatePlatformSmsSettingsRequest request = new UpdatePlatformSmsSettingsRequest();
        request.setWelcomeTemplate("Hi {name}");
        request.setEnabled(false);

        PlatformSmsSettings saved = service.update(request);

        assertEquals("Hi {name}", saved.getTemplateWelcome());
        assertEquals(false, saved.isEnabled());
        verify(repository).save(stored);
    }
}
