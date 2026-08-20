package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgNotificationServiceTest {

    @Mock
    private SmsProviderFactory smsProviderFactory;
    @Mock
    private SmsProvider smsProvider;

    @Mock
    private SmsSettingsService smsSettingsService;

    private AppProperties appProperties;
    private OrgNotificationService service;
    private Organization organization;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        AppProperties.Templates templates = appProperties.getNotifications().getTemplates();
        service = new OrgNotificationService(smsProviderFactory, appProperties, smsSettingsService);
        lenient().when(smsSettingsService.isEnabled()).thenReturn(true);
        lenient().when(smsSettingsService.lowBalanceThreshold()).thenReturn(new BigDecimal("50.00"));
        lenient().when(smsSettingsService.portalUrl()).thenReturn("https://novasms.novastack.co.ke");
        lenient().when(smsSettingsService.welcomeTemplate()).thenReturn(templates.getWelcome());
        lenient().when(smsSettingsService.topupTemplate()).thenReturn(templates.getTopup());
        lenient().when(smsSettingsService.collectionTemplate()).thenReturn(templates.getCollection());
        lenient().when(smsSettingsService.lowBalanceTemplate()).thenReturn(templates.getLowBalance());
        lenient().when(smsSettingsService.platformTopupTemplate()).thenReturn(templates.getPlatformTopup());
        lenient().when(smsSettingsService.providerLowTemplate()).thenReturn(templates.getProviderLow());
        lenient().when(smsSettingsService.providerExposureTemplate()).thenReturn(templates.getProviderExposure());
        lenient().when(smsSettingsService.collectionNotifyPhones())
                .thenReturn(List.of("0711766223", "0759728742"));
        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .phone("254711766223")
                .mpesaAccountRef("NOVAC727")
                .email("acme@example.com")
                .apiKey("nsk_test")
                .build();
    }

    @Test
    void welcomeSmsUsesConfiguredTemplate() {
        when(smsSettingsService.welcomeTemplate()).thenReturn("Hello {name}. Portal {portalUrl}");
        stubProvider();

        service.notifyWelcome(organization);

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider).send(captor.capture());
        assertEquals(
                "Hello Acme. Portal https://novasms.novastack.co.ke",
                captor.getValue().message());
    }

    @Test
    void welcomeSmsIsSentToOrganizationPhone() {
        stubProvider();

        service.notifyWelcome(organization);

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider).send(captor.capture());
        assertTrue(captor.getValue().recipient().equals("254711766223"));
        assertTrue(captor.getValue().message().contains("Welcome to Nova SMS, Acme"));
        assertTrue(captor.getValue().message().contains("Top up"));
    }

    @Test
    void topUpSmsIncludesAmountReceiptAndBalance() {
        stubProvider();

        service.notifyTopUpSuccess(organization, new BigDecimal("1.00"), new BigDecimal("11.00"), "UHJA53YW7O");

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider).send(captor.capture());
        String body = captor.getValue().message();
        assertTrue(body.contains("KES 1.00"));
        assertTrue(body.contains("UHJA53YW7O"));
        assertTrue(body.contains("KES 11.00"));
    }

    @Test
    void platformOwnerTopUpSmsGoesToNotifyPhones() {
        stubPlatformProvider();

        service.notifyPlatformOwnerTopUp(
                organization, new BigDecimal("1000.00"), new BigDecimal("1350.00"), "QWE123XYZ");

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider, times(2)).send(captor.capture());
        List<String> recipients = captor.getAllValues().stream().map(SmsProvider.SmsProviderRequest::recipient).toList();
        assertTrue(recipients.contains("254711766223"));
        assertTrue(recipients.contains("254759728742"));
        String body = captor.getAllValues().getFirst().message();
        assertTrue(body.contains("Acme"));
        assertTrue(body.contains("NOVAC727"));
        assertTrue(body.contains("KES 1,000"));
        assertTrue(body.contains("QWE123XYZ"));
        assertTrue(body.contains("KES 1,350"));
        assertTrue(body.matches("(?s).*\\d{2}/\\d{2} \\d{2}:\\d{2}.*"));
        assertTrue(body.length() <= 160);
    }

    @Test
    void platformOwnerTopUpSmsStaysWithinOneGsmSegment() {
        organization.setName("Nairobi Logistics and Freight Services International Limited");
        stubPlatformProvider();

        service.notifyPlatformOwnerTopUp(
                organization, new BigDecimal("1000000.00"), new BigDecimal("1250000.50"), "UHJA53YW7O");

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider, times(2)).send(captor.capture());
        String body = captor.getAllValues().getFirst().message();
        assertTrue(body.length() <= 160);
        assertTrue(body.contains("UHJA53YW7O"));
    }

    @Test
    void platformOwnerTopUpIgnoresOrgNotificationToggle() {
        organization.setNotificationsEnabled(false);
        stubPlatformProvider();

        service.notifyPlatformOwnerTopUp(
                organization, new BigDecimal("50.00"), new BigDecimal("60.00"), "ABC123");

        verify(smsProvider, times(2)).send(any());
    }

    @Test
    void platformOwnerTopUpDoesNotSendWhenPlatformSmsDisabled() {
        when(smsSettingsService.isEnabled()).thenReturn(false);

        service.notifyPlatformOwnerTopUp(
                organization, new BigDecimal("1.00"), new BigDecimal("11.00"), "UHJA53YW7O");

        verify(smsProviderFactory, never()).getDefaultProvider();
        verify(smsProviderFactory, never()).buildRequest(any(), any(), any(), any());
    }

    @Test
    void providerLowUnitsSmsGoesToPlatformOwnerPhones() {
        stubPlatformProvider();

        service.notifyProviderLowUnits(new BigDecimal("40.00"), new BigDecimal("50.00"));

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider, times(2)).send(captor.capture());
        String body = captor.getAllValues().getFirst().message();
        assertTrue(body.contains("40"));
        assertTrue(body.contains("50"));
        assertTrue(body.contains("TalkSasa"));
    }

    @Test
    void providerLowUnitsDoesNotSendWhenPlatformSmsDisabled() {
        when(smsSettingsService.isEnabled()).thenReturn(false);

        service.notifyProviderLowUnits(new BigDecimal("10.00"), new BigDecimal("50.00"));

        verify(smsProviderFactory, never()).getDefaultProvider();
        verify(smsProviderFactory, never()).buildRequest(any(), any(), any(), any());
    }

    @Test
    void walletExposureSmsGoesToPlatformOwnerPhones() {
        stubPlatformProvider();

        service.notifyWalletExposure(new BigDecimal("250.00"), new BigDecimal("100.00"));

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider, times(2)).send(captor.capture());
        String body = captor.getAllValues().getFirst().message();
        assertTrue(body.contains("250"));
        assertTrue(body.contains("100"));
    }

    @Test
    void lowBalanceSmsIncludesCurrentBalance() {
        stubProvider();

        service.notifyLowBalance(organization, new BigDecimal("45.00"));

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider).send(captor.capture());
        assertTrue(captor.getValue().message().contains("KES 45.00"));
        assertTrue(captor.getValue().message().contains("low"));
    }

    @Test
    void collectionSmsGoesToConfiguredPhonesWithPayerName() {
        when(smsSettingsService.collectionNotifyPhones()).thenReturn(List.of("0711766223", "0759728742"));
        when(smsProviderFactory.buildRequest(any(), any(), any(), any()))
                .thenAnswer(invocation -> new SmsProvider.SmsProviderRequest(
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(smsProviderFactory.getDefaultProvider()).thenReturn(smsProvider);
        when(smsProvider.send(any())).thenReturn(
                SmsProvider.SmsProviderResult.accepted("mid-1", "{}", "{}", 200));

        service.notifyCollectionReceived("SHEILA", new BigDecimal("100.00"), "Keneth", "Kip", "Kipyegon", "UHJA541HGH");

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider, times(2)).send(captor.capture());
        List<String> recipients = captor.getAllValues().stream().map(SmsProvider.SmsProviderRequest::recipient).toList();
        assertTrue(recipients.contains("254711766223"));
        assertTrue(recipients.contains("254759728742"));
        String body = captor.getAllValues().getFirst().message();
        assertTrue(body.contains("KES 100.00"));
        assertTrue(body.contains("Keneth Kip Kipyegon"));
        assertTrue(body.contains("for Sheila"));
        assertTrue(body.contains("UHJA541HGH"));
        assertFalse(body.contains("KENETH"));
        assertFalse(body.toLowerCase().contains("sheila/keneth"));
    }

    @Test
    void collectionSmsUsesKenethWhenThatAccountWasPaid() {
        when(smsSettingsService.collectionNotifyPhones()).thenReturn(List.of("0711766223"));
        when(smsProviderFactory.buildRequest(any(), any(), any(), any()))
                .thenAnswer(invocation -> new SmsProvider.SmsProviderRequest(
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(smsProviderFactory.getDefaultProvider()).thenReturn(smsProvider);
        when(smsProvider.send(any())).thenReturn(
                SmsProvider.SmsProviderResult.accepted("mid-1", "{}", "{}", 200));

        service.notifyCollectionReceived("KENETH", new BigDecimal("50.00"), "Jane", "", "Wanjiku", "ABC123");

        ArgumentCaptor<SmsProvider.SmsProviderRequest> captor =
                ArgumentCaptor.forClass(SmsProvider.SmsProviderRequest.class);
        verify(smsProvider).send(captor.capture());
        String body = captor.getValue().message();
        assertTrue(body.contains("for Keneth"));
        assertFalse(body.contains("Sheila"));
    }

    @Test
    void disabledNotificationsDoNotSend() {
        when(smsSettingsService.isEnabled()).thenReturn(false);

        service.notifyWelcome(organization);

        verify(smsProviderFactory, never()).getDefaultProvider();
        verify(smsProviderFactory, never()).buildRequest(any(), any(), any(), any());
    }

    @Test
    void orgDisabledNotificationsDoNotSend() {
        organization.setNotificationsEnabled(false);

        service.notifyWelcome(organization);

        verify(smsProviderFactory, never()).getDefaultProvider();
        verify(smsProviderFactory, never()).buildRequest(any(), any(), any(), any());
    }

    @Test
    void missingPhoneDoesNotSend() {
        organization.setPhone(null);

        service.notifyWelcome(organization);

        verify(smsProviderFactory, never()).getDefaultProvider();
    }

    @Test
    void providerFailureIsSwallowed() {
        when(smsProviderFactory.buildRequest(eq(organization), any(), any(), any()))
                .thenReturn(new SmsProvider.SmsProviderRequest("254711766223", "hello", "TALK-SASA"));
        when(smsProviderFactory.getDefaultProvider()).thenReturn(smsProvider);
        when(smsProvider.send(any())).thenThrow(new RuntimeException("provider down"));

        service.notifyWelcome(organization);
    }

    @Test
    void lowBalanceThresholdFiresOnlyOnCrossing() {
        BigDecimal threshold = new BigDecimal("50.00");
        assertTrue(service.crossedLowBalanceThreshold(new BigDecimal("51.00"), new BigDecimal("50.00"), threshold));
        assertTrue(service.crossedLowBalanceThreshold(new BigDecimal("60.00"), new BigDecimal("45.00"), threshold));
        assertFalse(service.crossedLowBalanceThreshold(new BigDecimal("50.00"), new BigDecimal("40.00"), threshold));
        assertFalse(service.crossedLowBalanceThreshold(new BigDecimal("40.00"), new BigDecimal("30.00"), threshold));
        assertFalse(service.crossedLowBalanceThreshold(new BigDecimal("80.00"), new BigDecimal("70.00"), threshold));
    }

    @Test
    void organizationThresholdOverridesPlatformDefault() {
        organization.setLowBalanceThreshold(new BigDecimal("200.00"));
        assertTrue(service.crossedLowBalanceThreshold(organization, new BigDecimal("250.00"), new BigDecimal("180.00")));
        assertFalse(service.crossedLowBalanceThreshold(organization, new BigDecimal("60.00"), new BigDecimal("45.00")));
        assertEquals(0, new BigDecimal("200.00").compareTo(service.resolveThreshold(organization)));
    }

    private void stubProvider() {
        when(smsProviderFactory.buildRequest(eq(organization), any(), any(), any()))
                .thenAnswer(invocation -> new SmsProvider.SmsProviderRequest(
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(smsProviderFactory.getDefaultProvider()).thenReturn(smsProvider);
        when(smsProvider.send(any())).thenReturn(
                SmsProvider.SmsProviderResult.accepted("mid-1", "{}", "{}", 200));
    }

    private void stubPlatformProvider() {
        when(smsProviderFactory.buildRequest(any(), any(), any(), any()))
                .thenAnswer(invocation -> new SmsProvider.SmsProviderRequest(
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3)));
        when(smsProviderFactory.getDefaultProvider()).thenReturn(smsProvider);
        when(smsProvider.send(any())).thenReturn(
                SmsProvider.SmsProviderResult.accepted("mid-1", "{}", "{}", 200));
    }
}
