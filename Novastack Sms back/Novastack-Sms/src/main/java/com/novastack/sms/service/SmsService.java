package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.ContactRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.dto.request.BulkSmsRequest;
import com.novastack.sms.dto.request.ScheduleSmsRequest;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.BulkSmsResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsMessageRepository smsMessageRepository;
    private final OrganizationRepository organizationRepository;
    private final ContactRepository contactRepository;
    private final WalletService walletService;
    private final SenderIdService senderIdService;
    private final SmsDeliveryService smsDeliveryService;

    @Transactional
    public SmsMessageResponse sendSingle(UUID organizationId, SendSmsRequest request) {
        walletService.assertSufficientBalance(organizationId, 1);
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());

        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));

        walletService.debitForSms(organizationId, org.getSmsCost(),
                "SMS-" + UUID.randomUUID(), "SMS to " + request.getRecipient());

        SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                .organization(org)
                .recipient(normalizePhone(request.getRecipient()))
                .content(request.getMessage())
                .senderId(sender)
                .status(MessageStatus.QUEUED)
                .cost(org.getSmsCost())
                .build());

        smsDeliveryService.processQueuedMessage(message.getId());
        return toResponse(smsMessageRepository.findById(message.getId()).orElse(message));
    }

    @Transactional
    public BulkSmsResponse sendBulk(UUID organizationId, BulkSmsRequest request) {
        Set<String> recipients = resolveRecipients(organizationId, request.getRecipients(), request.getGroupId());
        if (recipients.isEmpty()) {
            throw new ApiException("No recipients provided", HttpStatus.BAD_REQUEST);
        }

        walletService.assertSufficientBalance(organizationId, recipients.size());
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());
        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));

        UUID batchId = UUID.randomUUID();
        List<SmsMessageResponse> responses = new ArrayList<>();

        for (String recipient : recipients) {
            walletService.debitForSms(organizationId, org.getSmsCost(),
                    "SMS-" + UUID.randomUUID(), "Bulk SMS to " + recipient);

            SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                    .organization(org)
                    .recipient(normalizePhone(recipient))
                    .content(request.getMessage())
                    .senderId(sender)
                    .status(MessageStatus.QUEUED)
                    .cost(org.getSmsCost())
                    .batchId(batchId)
                    .build());

            smsDeliveryService.processQueuedMessage(message.getId());
            responses.add(toResponse(smsMessageRepository.findById(message.getId()).orElse(message)));
        }

        return BulkSmsResponse.builder()
                .batchId(batchId)
                .queuedCount(responses.size())
                .messages(responses)
                .build();
    }

    @Transactional
    public BulkSmsResponse schedule(UUID organizationId, ScheduleSmsRequest request) {
        Set<String> recipients = resolveRecipients(organizationId, request.getRecipients(), request.getGroupId());
        if (recipients.isEmpty()) {
            throw new ApiException("No recipients provided", HttpStatus.BAD_REQUEST);
        }

        walletService.assertSufficientBalance(organizationId, recipients.size());
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());
        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));

        UUID batchId = UUID.randomUUID();
        List<SmsMessageResponse> responses = new ArrayList<>();

        for (String recipient : recipients) {
            walletService.debitForSms(organizationId, org.getSmsCost(),
                    "SMS-" + UUID.randomUUID(), "Scheduled SMS to " + recipient);

            SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                    .organization(org)
                    .recipient(normalizePhone(recipient))
                    .content(request.getMessage())
                    .senderId(sender)
                    .status(MessageStatus.SCHEDULED)
                    .cost(org.getSmsCost())
                    .batchId(batchId)
                    .scheduledAt(request.getScheduledAt())
                    .build());

            responses.add(toResponse(message));
        }

        return BulkSmsResponse.builder()
                .batchId(batchId)
                .queuedCount(responses.size())
                .messages(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<SmsMessageResponse> history(UUID organizationId, Pageable pageable) {
        return smsMessageRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SmsMessageResponse> platformHistory(Pageable pageable) {
        return smsMessageRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    private Set<String> resolveRecipients(UUID organizationId, List<String> recipients, UUID groupId) {
        Set<String> result = new LinkedHashSet<>();
        if (recipients != null) {
            recipients.stream().filter(r -> r != null && !r.isBlank()).forEach(result::add);
        }
        if (groupId != null) {
            contactRepository.findByOrganizationIdAndGroupsId(organizationId, groupId)
                    .forEach(c -> result.add(c.getPhone()));
        }
        return result.stream().map(this::normalizePhone).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            cleaned = "254" + cleaned.substring(1);
        }
        return cleaned;
    }

    public SmsMessageResponse toResponse(SmsMessage message) {
        return SmsMessageResponse.builder()
                .id(message.getId())
                .recipient(message.getRecipient())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .status(message.getStatus())
                .cost(message.getCost())
                .batchId(message.getBatchId())
                .scheduledAt(message.getScheduledAt())
                .createdAt(message.getCreatedAt())
                .sentAt(message.getSentAt())
                .deliveredAt(message.getDeliveredAt())
                .failureReason(message.getFailureReason())
                .build();
    }
}
