package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.ContactRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.dto.request.BulkSmsRequest;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.BulkSmsResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceResendFailedTest {

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
    private UUID orgId;
    private UUID batchId;

    @BeforeEach
    void setUp() {
        smsService = spy(new SmsService(
                smsMessageRepository,
                organizationRepository,
                contactRepository,
                walletService,
                senderIdService,
                smsDeliveryService,
                smsStatusService,
                smsBillingCalculator));
        orgId = UUID.randomUUID();
        batchId = UUID.randomUUID();
    }

    @Test
    void resendFailedSendsOnlyFailedRecipients() {
        when(smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, orgId)).thenReturn(List.of(
                message(MessageStatus.SENT, "254711000001"),
                message(MessageStatus.FAILED, "254711000002"),
                message(MessageStatus.DELIVERED, "254711000003"),
                message(MessageStatus.FAILED, "254711000004"),
                message(MessageStatus.PENDING, "254711000005"),
                message(MessageStatus.ACCEPTED, "254711000006")));
        UUID newBatchId = UUID.randomUUID();
        doReturn(BulkSmsResponse.builder()
                .batchId(newBatchId)
                .queuedCount(2)
                .recipientCount(2)
                .status("PROCESSING")
                .messages(List.of())
                .build()).when(smsService).sendBulk(eq(orgId), any(BulkSmsRequest.class), eq(MessageChannel.SMS));

        BulkSmsResponse result = smsService.resendFailed(orgId, batchId);

        ArgumentCaptor<BulkSmsRequest> captor = ArgumentCaptor.forClass(BulkSmsRequest.class);
        verify(smsService).sendBulk(eq(orgId), captor.capture(), eq(MessageChannel.SMS));
        assertEquals(List.of("254711000002", "254711000004"), captor.getValue().getRecipients());
        assertEquals("Keep sending", captor.getValue().getMessage());
        assertEquals("NOVASMS", captor.getValue().getSenderId());
        assertEquals(newBatchId, result.getBatchId());
        assertEquals(batchId, result.getSourceBatchId());
        assertEquals(2, result.getResentCount());
        assertEquals(4, result.getSkippedCount());
        verify(smsMessageRepository, never()).save(any());
    }

    @Test
    void resendFailedIncludesRejectedAndCancelled() {
        when(smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, orgId)).thenReturn(List.of(
                message(MessageStatus.REJECTED, "254711000002"),
                message(MessageStatus.CANCELLED, "254711000004")));
        doReturn(BulkSmsResponse.builder()
                .batchId(UUID.randomUUID())
                .queuedCount(2)
                .recipientCount(2)
                .messages(List.of())
                .build()).when(smsService).sendBulk(eq(orgId), any(BulkSmsRequest.class), eq(MessageChannel.SMS));

        smsService.resendFailed(orgId, batchId);

        ArgumentCaptor<BulkSmsRequest> captor = ArgumentCaptor.forClass(BulkSmsRequest.class);
        verify(smsService).sendBulk(eq(orgId), captor.capture(), eq(MessageChannel.SMS));
        assertEquals(List.of("254711000002", "254711000004"), captor.getValue().getRecipients());
    }

    @Test
    void resendFailedThrowsWhenNothingFailed() {
        when(smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, orgId)).thenReturn(List.of(
                message(MessageStatus.SENT, "254711000001"),
                message(MessageStatus.DELIVERED, "254711000003")));

        ApiException ex = assertThrows(ApiException.class, () -> smsService.resendFailed(orgId, batchId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("No failed messages to resend", ex.getMessage());
        verify(smsService, never()).sendBulk(any(), any(), any());
    }

    @Test
    void resendFailedThrowsWhenBatchMissing() {
        when(smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, orgId)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> smsService.resendFailed(orgId, batchId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void resendSingleCreatesNewSendAndLeavesOriginal() {
        UUID messageId = UUID.randomUUID();
        SmsMessage original = message(MessageStatus.FAILED, "254711000002");
        original.setId(messageId);
        when(smsMessageRepository.findByIdAndOrganization_Id(messageId, orgId)).thenReturn(Optional.of(original));
        SmsMessageResponse sent = SmsMessageResponse.builder()
                .id(UUID.randomUUID())
                .recipient("254711000002")
                .status(MessageStatus.ACCEPTED)
                .build();
        doReturn(sent).when(smsService).sendSingle(eq(orgId), any(SendSmsRequest.class), eq(MessageChannel.SMS));

        SmsMessageResponse result = smsService.resend(orgId, messageId);

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(smsService).sendSingle(eq(orgId), captor.capture(), eq(MessageChannel.SMS));
        assertEquals("254711000002", captor.getValue().getRecipient());
        assertEquals("Keep sending", captor.getValue().getMessage());
        assertEquals(sent.getId(), result.getId());
        verify(smsMessageRepository, never()).save(original);
    }

    @Test
    void getBatchIncludesFailedCountAndDoesNotResend() {
        when(smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, orgId)).thenReturn(List.of(
                message(MessageStatus.SENT, "254711000001"),
                message(MessageStatus.FAILED, "254711000002")));

        BulkSmsResponse result = smsService.getBatchForOrganization(orgId, batchId);

        assertEquals(batchId, result.getBatchId());
        assertEquals(2, result.getRecipientCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("COMPLETED", result.getStatus());
        verify(smsService, never()).sendBulk(any(), any(), any());
    }

    @Test
    void resendSingleRejectsSuccessfulMessages() {
        UUID messageId = UUID.randomUUID();
        SmsMessage original = message(MessageStatus.SENT, "254711000001");
        when(smsMessageRepository.findByIdAndOrganization_Id(messageId, orgId)).thenReturn(Optional.of(original));

        ApiException ex = assertThrows(ApiException.class, () -> smsService.resend(orgId, messageId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(smsService, never()).sendSingle(any(), any(), any());
    }

    private static SmsMessage message(MessageStatus status, String recipient) {
        SmsMessage message = new SmsMessage();
        message.setId(UUID.randomUUID());
        message.setRecipient(recipient);
        message.setContent("Keep sending");
        message.setSenderId("NOVASMS");
        message.setStatus(status);
        message.setChannel(MessageChannel.SMS);
        return message;
    }
}
