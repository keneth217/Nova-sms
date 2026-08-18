package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import com.novastack.sms.provider.TalkSasaSmsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsStatusServiceTest {

    @Mock
    private SmsMessageRepository smsMessageRepository;
    @Mock
    private SmsProviderFactory smsProviderFactory;
    @Mock
    private SmsProvider provider;

    private SmsStatusService service;

    @BeforeEach
    void setUp() {
        service = new SmsStatusService(smsMessageRepository, smsProviderFactory);
        when(smsProviderFactory.getProvider("talksasa")).thenReturn(provider);
        when(provider.supportsStatusLookup()).thenReturn(true);
    }

    @Test
    void completedQueueUpdatesAcceptedMessageToDeliveredUsingCompletedAt() {
        UUID messageId = UUID.fromString("b00f285f-2059-4945-8827-682554a2260e");
        Instant completedAt = Instant.parse("2026-08-18T14:45:41.000000Z");
        SmsMessage message = new SmsMessage();
        message.setId(messageId);
        message.setStatus(MessageStatus.ACCEPTED);
        message.setProvider(TalkSasaSmsProvider.PROVIDER_NAME);
        message.setProviderMessageId("58ef3d58-77ae-4dcb-b633-ee9a02a26c9f");

        when(provider.getSmsStatus("58ef3d58-77ae-4dcb-b633-ee9a02a26c9f")).thenReturn(
                SmsProvider.SmsStatusResult.of(
                        MessageStatus.DELIVERED,
                        "completed",
                        "58ef3d58-77ae-4dcb-b633-ee9a02a26c9f",
                        completedAt,
                        1,
                        0,
                        0));

        service.syncMessage(message);

        ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsMessageRepository).save(captor.capture());
        SmsMessage saved = captor.getValue();
        assertEquals(messageId, saved.getId());
        assertEquals(MessageStatus.DELIVERED, saved.getStatus());
        assertEquals(completedAt, saved.getDeliveredAt());
        assertNull(saved.getFailureReason());
    }
}
