package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PlatformBillingSettings;
import com.novastack.sms.domain.repository.PlatformBillingSettingsRepository;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.provider.TalkSasaSmsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingSettingsServiceTest {

    @Mock
    private PlatformBillingSettingsRepository repository;

    private BillingSettingsService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getSms().getBilling().setCustomerPrice(new BigDecimal("1.00"));
        properties.getSms().getBilling().setProviderCost(new BigDecimal("0.35"));
        properties.getSms().getBilling().setCurrency("KES");
        service = new BillingSettingsService(repository, properties);
    }

    @Test
    void defaultsAreCustomerOneAndProviderThirtyFive() {
        PlatformBillingSettings settings = new PlatformBillingSettings();
        settings.setId(PlatformBillingSettings.SINGLETON_ID);
        settings.setCustomerPrice(new BigDecimal("1.00"));
        settings.setProviderCost(new BigDecimal("0.35"));
        settings.setCurrency("KES");
        when(repository.findById(PlatformBillingSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));

        assertEquals(0, service.customerPrice().compareTo(new BigDecimal("1.00")));
        assertEquals(0, service.providerCostPerSms().compareTo(new BigDecimal("0.35")));
        assertEquals("KES", service.currency());
        assertEquals(0, service.customerPrice().subtract(service.providerCostPerSms())
                .compareTo(new BigDecimal("0.65")));
    }

    @Test
    void rejectsForbiddenProviderCost() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.update(new BigDecimal("1.00"), new BigDecimal("0.357"), "KES"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void providerNameIsTalksasaAndSenderIsTalkSasa() {
        assertEquals("TALKSASA", TalkSasaSmsProvider.PROVIDER_NAME);
        assertEquals("TALK-SASA", AppProperties.TalkSasa.DEFAULT_SENDER_ID);
        assertEquals("TALK-SASA", new AppProperties().getSms().getTalksasa().resolvedDefaultSenderId());
    }

    @Test
    void configurationDoesNotUseForbiddenProviderCost() throws Exception {
        Path yaml = Path.of("src/main/resources/application.yaml");
        Path envExample = Path.of(".env.example");
        assertFalse(Files.readString(yaml).contains("0.357"));
        if (Files.exists(envExample)) {
            assertFalse(Files.readString(envExample).contains("0.357"));
        }
    }
}
