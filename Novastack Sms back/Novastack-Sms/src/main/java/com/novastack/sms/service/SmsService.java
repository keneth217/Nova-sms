package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.enums.MessageChannel;
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
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.util.PhoneNormalizer;
import com.novastack.sms.util.SmsSegmentCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final SmsStatusService smsStatusService;
    private final SmsBillingCalculator smsBillingCalculator;

    @Transactional
    public SmsMessageResponse sendSingle(UUID organizationId, SendSmsRequest request) {
        return sendSingle(organizationId, request, MessageChannel.SMS);
    }

    @Transactional
    public SmsMessageResponse sendSingle(UUID organizationId, SendSmsRequest request, MessageChannel channel) {
        return sendSingle(organizationId, request, channel, currentApiClientId());
    }

    @Transactional
    public SmsMessageResponse sendSingle(
            UUID organizationId,
            SendSmsRequest request,
            MessageChannel channel,
            UUID apiClientId) {
        MessageChannel resolved = channel == null ? MessageChannel.SMS : channel;
        String recipient = PhoneNormalizer.normalizeKenyanMobile(request.getRecipient());
        int units = requireUnits(request.getMessage(), resolved);
        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        SmsBillingQuote quote = smsBillingCalculator.quote(units);
        BigDecimal totalCost = quote.customerCharge();

        walletService.assertSufficientAmount(organizationId, totalCost);
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());
        walletService.debitForSms(organizationId, totalCost,
                resolved.name() + "-" + UUID.randomUUID(),
                resolved.displayName() + " to " + recipient + " (" + units + " unit" + (units == 1 ? "" : "s") + ")");

        SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                .organization(org)
                .apiClientId(apiClientId)
                .recipient(recipient)
                .content(request.getMessage())
                .channel(resolved)
                .senderId(sender)
                .status(MessageStatus.PENDING)
                .cost(quote.customerCharge())
                .smsUnits(units)
                .encoding(encoding(request.getMessage(), resolved))
                .characterCount(characterCount(request.getMessage()))
                .unitPrice(quote.customerPrice())
                .providerCost(quote.providerCost())
                .grossMargin(quote.grossMargin())
                .billingStatus(com.novastack.sms.domain.enums.BillingStatus.CHARGED)
                .currency(quote.currency())
                .provider(providerName(resolved))
                .build());

        smsDeliveryService.processQueuedMessage(message.getId());
        return toResponse(smsMessageRepository.findById(message.getId()).orElse(message));
    }

    @Transactional
    public BulkSmsResponse sendBulk(UUID organizationId, BulkSmsRequest request) {
        return sendBulk(organizationId, request, MessageChannel.SMS);
    }

    @Transactional
    public BulkSmsResponse sendBulk(UUID organizationId, BulkSmsRequest request, MessageChannel channel) {
        MessageChannel resolved = channel == null ? MessageChannel.SMS : channel;
        Set<String> recipients = resolveRecipients(organizationId, request.getRecipients(), request.getGroupId());
        if (recipients.isEmpty()) {
            throw new ApiException("No recipients provided", HttpStatus.BAD_REQUEST);
        }

        int units = requireUnits(request.getMessage(), resolved);
        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        SmsBillingQuote quote = smsBillingCalculator.quote(units);
        BigDecimal totalCost = quote.customerCharge().multiply(BigDecimal.valueOf(recipients.size()));

        walletService.assertSufficientAmount(organizationId, totalCost);
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());

        UUID batchId = UUID.randomUUID();
        List<SmsMessageResponse> responses = new ArrayList<>();
        List<UUID> messageIds = new ArrayList<>();
        String providerName = providerName(resolved);

        for (String recipient : recipients) {
            walletService.debitForSms(organizationId, quote.customerCharge(),
                    resolved.name() + "-" + UUID.randomUUID(),
                    "Bulk " + resolved.displayName() + " to " + recipient);

            SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                    .organization(org)
                    .apiClientId(currentApiClientId())
                    .recipient(recipient)
                    .content(request.getMessage())
                    .channel(resolved)
                    .senderId(sender)
                    .status(MessageStatus.PENDING)
                    .cost(quote.customerCharge())
                    .smsUnits(units)
                    .encoding(encoding(request.getMessage(), resolved))
                    .characterCount(characterCount(request.getMessage()))
                    .unitPrice(quote.customerPrice())
                    .providerCost(quote.providerCost())
                    .grossMargin(quote.grossMargin())
                    .billingStatus(com.novastack.sms.domain.enums.BillingStatus.CHARGED)
                    .currency(quote.currency())
                    .provider(providerName)
                    .batchId(batchId)
                    .build());
            messageIds.add(message.getId());
        }

        smsDeliveryService.processQueuedBatch(batchId);

        for (UUID messageId : messageIds) {
            responses.add(toResponse(smsMessageRepository.findById(messageId)
                    .orElseThrow(() -> new ApiException("SMS message not found", HttpStatus.NOT_FOUND))));
        }

        return BulkSmsResponse.builder()
                .batchId(batchId)
                .queuedCount(responses.size())
                .recipientCount(responses.size())
                .smsUnits(units * responses.size())
                .status("PROCESSING")
                .messages(responses)
                .build();
    }

    @Transactional
    public BulkSmsResponse schedule(UUID organizationId, ScheduleSmsRequest request) {
        return schedule(organizationId, request, MessageChannel.SMS);
    }

    @Transactional
    public BulkSmsResponse schedule(UUID organizationId, ScheduleSmsRequest request, MessageChannel channel) {
        MessageChannel resolved = channel == null ? MessageChannel.SMS : channel;
        Set<String> recipients = resolveRecipients(organizationId, request.getRecipients(), request.getGroupId());
        if (recipients.isEmpty()) {
            throw new ApiException("No recipients provided", HttpStatus.BAD_REQUEST);
        }

        int units = requireUnits(request.getMessage(), resolved);
        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        SmsBillingQuote quote = smsBillingCalculator.quote(units);
        BigDecimal totalCost = quote.customerCharge().multiply(BigDecimal.valueOf(recipients.size()));

        walletService.assertSufficientAmount(organizationId, totalCost);
        String sender = senderIdService.resolveApprovedSender(organizationId, request.getSenderId());

        UUID batchId = UUID.randomUUID();
        List<SmsMessageResponse> responses = new ArrayList<>();
        String providerName = providerName(resolved);

        for (String recipient : recipients) {
            walletService.debitForSms(organizationId, quote.customerCharge(),
                    resolved.name() + "-" + UUID.randomUUID(),
                    "Scheduled " + resolved.displayName() + " to " + recipient);

            SmsMessage message = smsMessageRepository.save(SmsMessage.builder()
                    .organization(org)
                    .apiClientId(currentApiClientId())
                    .recipient(recipient)
                    .content(request.getMessage())
                    .channel(resolved)
                    .senderId(sender)
                    .status(MessageStatus.SCHEDULED)
                    .cost(quote.customerCharge())
                    .smsUnits(units)
                    .encoding(encoding(request.getMessage(), resolved))
                    .characterCount(characterCount(request.getMessage()))
                    .unitPrice(quote.customerPrice())
                    .providerCost(quote.providerCost())
                    .grossMargin(quote.grossMargin())
                    .billingStatus(com.novastack.sms.domain.enums.BillingStatus.CHARGED)
                    .currency(quote.currency())
                    .provider(providerName)
                    .scheduleOwner("NOVA")
                    .batchId(batchId)
                    .scheduledAt(request.getScheduledAt())
                    .build());

            responses.add(toResponse(message));
        }

        return BulkSmsResponse.builder()
                .batchId(batchId)
                .queuedCount(responses.size())
                .recipientCount(responses.size())
                .smsUnits(units * responses.size())
                .status("SCHEDULED")
                .messages(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<SmsMessageResponse> history(UUID organizationId, Pageable pageable) {
        return history(organizationId, pageable, MessageChannel.SMS);
    }

    @Transactional(readOnly = true)
    public Page<SmsMessageResponse> history(UUID organizationId, Pageable pageable, MessageChannel channel) {
        MessageChannel resolved = channel == null ? MessageChannel.SMS : channel;
        return smsMessageRepository
                .findByOrganizationIdAndChannelOrderByCreatedAtDesc(organizationId, resolved, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SmsMessageResponse> platformHistory(Pageable pageable) {
        return smsMessageRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SmsMessageResponse getForOrganization(UUID organizationId, UUID messageId) {
        return getForOrganization(organizationId, messageId, null);
    }

    @Transactional(readOnly = true)
    public SmsMessageResponse getForOrganization(UUID organizationId, UUID messageId, MessageChannel channel) {
        SmsMessage message = smsMessageRepository.findByIdAndOrganization_Id(messageId, organizationId)
                .orElseThrow(() -> new ApiException("SMS message not found", HttpStatus.NOT_FOUND));
        if (channel != null && message.getChannel() != null && message.getChannel() != channel) {
            throw new ApiException("SMS message not found", HttpStatus.NOT_FOUND);
        }
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public BulkSmsResponse getBatchForOrganization(UUID organizationId, UUID batchId) {
        List<SmsMessage> messages = smsMessageRepository.findByBatchIdAndOrganization_Id(batchId, organizationId);
        if (messages.isEmpty()) {
            throw new ApiException("SMS batch not found", HttpStatus.NOT_FOUND);
        }
        int units = messages.stream().mapToInt(SmsMessage::getSmsUnits).sum();
        return BulkSmsResponse.builder()
                .batchId(batchId)
                .queuedCount(messages.size())
                .recipientCount(messages.size())
                .smsUnits(units)
                .status("PROCESSING")
                .messages(messages.stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional
    public SmsMessageResponse refreshStatusById(UUID messageId) {
        SmsMessage message = smsMessageRepository.findByIdWithOrganization(messageId)
                .orElseThrow(() -> new ApiException("SMS message not found", HttpStatus.NOT_FOUND));
        return refreshStatus(message.getOrganization().getId(), messageId, message.getChannel());
    }

    @Transactional
    public SmsMessageResponse refreshStatus(UUID organizationId, UUID messageId) {
        return refreshStatus(organizationId, messageId, null);
    }

    @Transactional
    public SmsMessageResponse refreshStatus(UUID organizationId, UUID messageId, MessageChannel channel) {
        SmsMessage message = smsMessageRepository.findByIdAndOrganization_Id(messageId, organizationId)
                .orElseThrow(() -> new ApiException("SMS message not found", HttpStatus.NOT_FOUND));
        if (channel != null && message.getChannel() != null && message.getChannel() != channel) {
            throw new ApiException("SMS message not found", HttpStatus.NOT_FOUND);
        }
        smsStatusService.syncMessage(message);
        return toResponse(smsMessageRepository.findById(message.getId()).orElse(message));
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
        return result.stream()
                .map(PhoneNormalizer::normalizeKenyanMobile)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int requireUnits(String message, MessageChannel channel) {
        if (channel == MessageChannel.WHATSAPP) {
            if (message == null || message.isBlank()) {
                throw new ApiException("Message is required", HttpStatus.BAD_REQUEST);
            }
            return 1;
        }
        int units = SmsSegmentCalculator.units(message);
        if (units <= 0) {
            throw new ApiException("Message is required", HttpStatus.BAD_REQUEST);
        }
        return units;
    }

    private String providerName(MessageChannel channel) {
        if (channel == MessageChannel.WHATSAPP) {
            return com.novastack.sms.provider.TalkSasaSmsProvider.PROVIDER_NAME;
        }
        return smsDeliveryService.currentProviderName();
    }

    private UUID currentApiClientId() {
        return SecurityUtils.optionalApiClientId().orElse(null);
    }

    private String encoding(String message, MessageChannel channel) {
        if (channel == MessageChannel.WHATSAPP) {
            return "TEXT";
        }
        return SmsSegmentCalculator.analyze(message).encoding().name();
    }

    private int characterCount(String message) {
        return message == null ? 0 : message.length();
    }

    public SmsMessageResponse toResponse(SmsMessage message) {
        var organization = message.getOrganization();
        return SmsMessageResponse.builder()
                .id(message.getId())
                .messageId(message.getId() != null ? message.getId().toString() : null)
                .organizationId(organization != null ? organization.getId() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .apiClientId(message.getApiClientId())
                .recipient(message.getRecipient())
                .content(message.getContent())
                .channel(message.getChannel() == null ? MessageChannel.SMS : message.getChannel())
                .senderId(message.getSenderId())
                .status(message.getStatus())
                .cost(message.getCost())
                .smsUnits(message.getSmsUnits())
                .encoding(message.getEncoding())
                .characterCount(message.getCharacterCount())
                .unitPrice(message.getUnitPrice())
                .currency(message.getCurrency())
                .provider(message.getProvider())
                .batchId(message.getBatchId())
                .scheduledAt(message.getScheduledAt())
                .createdAt(message.getCreatedAt())
                .sentAt(message.getSentAt())
                .deliveredAt(message.getDeliveredAt())
                .failureReason(message.getFailureReason())
                .build();
    }
}
