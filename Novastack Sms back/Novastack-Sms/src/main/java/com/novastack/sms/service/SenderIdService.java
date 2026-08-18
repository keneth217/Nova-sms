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
import java.util.Optional;
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

    @Transactional(readOnly = true)
    public List<SenderId> listAll(SenderIdStatus status) {
        if (status == null) {
            return senderIdRepository.findAllByOrderByUpdatedAtDesc();
        }
        return senderIdRepository.findByStatusOrderByUpdatedAtDesc(status);
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
    public String resolveApprovedSender(UUID organizationId, String requestedSenderId) {
        if (requestedSenderId != null && !requestedSenderId.isBlank()) {
            String name = requestedSenderId.trim();
            Optional<SenderId> orgSender = senderIdRepository
                    .findByOrganizationIdAndSenderNameIgnoreCase(organizationId, name);
            if (orgSender.isPresent() && orgSender.get().getStatus() == SenderIdStatus.APPROVED) {
                return orgSender.get().getSenderName();
            }
            Optional<SenderId> platform = senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED);
            if (platform.isPresent() && platform.get().getSenderName().equalsIgnoreCase(name)) {
                return platform.get().getSenderName();
            }
            if (isTalksasa() && isTalksasaDefaultAlias(name)) {
                return talksasaDefaultSender();
            }
            throw new ApiException("Sender ID is not approved for this organization", HttpStatus.BAD_REQUEST);
        }
        if (isTalksasa()) {
            return talksasaDefaultSender();
        }
        return senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED)
                .map(SenderId::getSenderName)
                .or(() -> senderIdRepository
                        .findFirstByOrganizationIdAndStatusAndPlatformDefaultFalseOrderByCreatedAtAsc(
                                organizationId, SenderIdStatus.APPROVED)
                        .map(SenderId::getSenderName))
                .orElseGet(() -> appProperties.getSms().getPlatformSenderId().toUpperCase());
    }

    private boolean isTalksasa() {
        String provider = appProperties.getSms().getProvider();
        return provider == null || provider.isBlank() || "talksasa".equalsIgnoreCase(provider.trim());
    }

    private String talksasaDefaultSender() {
        return appProperties.getSms().getTalksasa().resolvedDefaultSenderId();
    }

    private boolean isTalksasaDefaultAlias(String name) {
        String configured = talksasaDefaultSender();
        return configured.equalsIgnoreCase(name)
                || "TALK-SASA".equalsIgnoreCase(name)
                || "TALK_SASA".equalsIgnoreCase(name);
    }
}
