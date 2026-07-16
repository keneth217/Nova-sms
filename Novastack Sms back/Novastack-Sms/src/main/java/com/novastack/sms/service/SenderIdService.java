package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SenderIdRepository;
import com.novastack.sms.dto.request.SenderIdRequest;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SenderIdService {

    private final SenderIdRepository senderIdRepository;
    private final OrganizationRepository organizationRepository;
    private final AppProperties appProperties;

    @Transactional
    public SenderId requestSenderId(UUID organizationId, SenderIdRequest request) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));

        senderIdRepository.findByOrganizationIdAndSenderNameIgnoreCase(organizationId, request.getSenderName())
                .ifPresent(s -> {
                    throw new ApiException("Sender ID already requested", HttpStatus.CONFLICT);
                });

        return senderIdRepository.save(SenderId.builder()
                .organization(organizationRepository.getReferenceById(organizationId))
                .senderName(request.getSenderName().toUpperCase())
                .status(SenderIdStatus.PENDING)
                .platformDefault(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SenderId> listForOrganization(UUID organizationId) {
        return senderIdRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public SenderId review(UUID senderId, SenderIdStatus status, String reason) {
        SenderId entity = senderIdRepository.findById(senderId)
                .orElseThrow(() -> new ApiException("Sender ID not found", HttpStatus.NOT_FOUND));
        entity.setStatus(status);
        entity.setReason(reason);
        return senderIdRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public String resolveApprovedSender(UUID organizationId, String requestedSender) {
        if (requestedSender == null || requestedSender.isBlank()) {
            return senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED)
                    .map(SenderId::getSenderName)
                    .orElse(appProperties.getSms().getPlatformSenderId());
        }

        String normalized = requestedSender.trim();
        boolean owned = senderIdRepository.existsByOrganizationIdAndSenderNameIgnoreCaseAndStatus(
                organizationId, normalized, SenderIdStatus.APPROVED);
        if (owned) {
            return normalized.toUpperCase();
        }

        boolean isPlatform = senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED)
                .map(s -> s.getSenderName().equalsIgnoreCase(normalized))
                .orElse(appProperties.getSms().getPlatformSenderId().equalsIgnoreCase(normalized));

        if (isPlatform) {
            return normalized.toUpperCase();
        }

        throw new ApiException("Sender ID not approved for this organization: " + requestedSender,
                HttpStatus.FORBIDDEN);
    }
}
