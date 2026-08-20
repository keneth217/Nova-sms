package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PaybillCollection;
import com.novastack.sms.domain.repository.PaybillCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaybillCollectionServiceTest {

    @Mock
    private PaybillCollectionRepository paybillCollectionRepository;
    @Mock
    private OrgNotificationService orgNotificationService;
    @Mock
    private SmsSettingsService smsSettingsService;

    private PaybillCollectionService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        service = new PaybillCollectionService(
                paybillCollectionRepository, properties, orgNotificationService, smsSettingsService);
        lenient().when(smsSettingsService.collectionAccounts()).thenReturn(List.of("SHEILA", "KENETH"));
    }

    @Test
    void collectionAccountsMatchCaseInsensitively() {
        assertTrue(service.isCollectionAccount("sheila"));
        assertTrue(service.isCollectionAccount("Sheila"));
        assertTrue(service.isCollectionAccount("SHEILA"));
        assertTrue(service.isCollectionAccount("keneth"));
        assertTrue(service.isCollectionAccount("Keneth"));
        assertTrue(service.isCollectionAccount("KENETH"));
        assertFalse(service.isCollectionAccount("NOVAC727"));
    }

    @Test
    void recordPersistsWithoutDuplicatingReceipt() {
        when(paybillCollectionRepository.existsByMpesaReceipt("UHJA541HGH")).thenReturn(false);
        when(paybillCollectionRepository.save(any(PaybillCollection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.record(
                "UHJA541HGH",
                new BigDecimal("100.00"),
                "sheila",
                "94c392c311d522da950619227b3361752a42042db7e1e699b26e628305c68a88",
                "20260819204838",
                "Keneth",
                "",
                "Kipyegon");

        ArgumentCaptor<PaybillCollection> captor = ArgumentCaptor.forClass(PaybillCollection.class);
        verify(paybillCollectionRepository).save(captor.capture());
        PaybillCollection saved = captor.getValue();
        assertEquals("SHEILA", saved.getBillRef());
        assertEquals("UHJA541HGH", saved.getMpesaReceipt());
        assertEquals("Keneth Kipyegon", saved.getPayerName());
        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getAmount()));
        verify(orgNotificationService).notifyCollectionReceived(
                eq("SHEILA"),
                any(),
                eq("Keneth"),
                eq(""),
                eq("Kipyegon"),
                eq("UHJA541HGH"));
    }

    @Test
    void duplicateReceiptIsIgnored() {
        when(paybillCollectionRepository.existsByMpesaReceipt("UHJA541HGH")).thenReturn(true);

        service.record("UHJA541HGH", new BigDecimal("100.00"), "SHEILA", null, null, null, null, null);

        verify(paybillCollectionRepository, never()).save(any());
        verify(orgNotificationService, never()).notifyCollectionReceived(any(), any(), any(), any(), any(), any());
    }
}
