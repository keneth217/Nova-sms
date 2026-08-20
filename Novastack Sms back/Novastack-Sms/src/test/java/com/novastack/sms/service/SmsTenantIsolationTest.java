package com.novastack.sms.service;

import com.novastack.sms.domain.repository.ContactRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsTenantIsolationTest {

    @Mock
    private SmsMessageRepository smsMessageRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private SenderIdService senderIdService;
    @Mock
    private SmsDeliveryService smsDeliveryService;
    @Mock
    private SmsStatusService smsStatusService;
    @Mock
    private SmsBillingCalculator smsBillingCalculator;

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsService(
                smsMessageRepository,
                organizationRepository,
                contactRepository,
                walletService,
                senderIdService,
                smsDeliveryService,
                smsStatusService,
                smsBillingCalculator);
    }

    @Test
    void organizationCannotReadAnotherOrganizationsSms() {
        UUID orgA = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        when(smsMessageRepository.findByIdAndOrganization_Id(messageId, orgA)).thenReturn(Optional.empty());
        when(smsMessageRepository.findByOrganization_IdAndProviderMessageId(orgA, messageId.toString()))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> smsService.getForOrganization(orgA, messageId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void organizationCannotReadAnotherOrganizationsTalkSasaUid() {
        UUID orgA = UUID.randomUUID();
        String uid = "606812e63f78b";

        when(smsMessageRepository.findByOrganization_IdAndProviderMessageId(orgA, uid))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> smsService.getForOrganization(orgA, uid));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
