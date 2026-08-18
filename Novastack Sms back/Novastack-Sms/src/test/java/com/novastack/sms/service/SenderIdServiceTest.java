package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SenderIdRepository;
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
class SenderIdServiceTest {

    @Mock
    private SenderIdRepository senderIdRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private AppProperties properties;
    private SenderIdService service;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getSms().setProvider("talksasa");
        properties.getSms().setPlatformSenderId("NOVASTACK");
        service = new SenderIdService(senderIdRepository, organizationRepository, properties);
        orgId = UUID.randomUUID();
    }

    @Test
    void requestedApprovedOrgSenderIsUsed() {
        SenderId approved = SenderId.builder()
                .senderName("MYCOMPANY")
                .status(SenderIdStatus.APPROVED)
                .build();
        when(senderIdRepository.findByOrganizationIdAndSenderNameIgnoreCase(orgId, "MYCOMPANY"))
                .thenReturn(Optional.of(approved));

        assertEquals("MYCOMPANY", service.resolveApprovedSender(orgId, "MYCOMPANY"));
    }

    @Test
    void unapprovedRequestedSenderIsRejected() {
        when(senderIdRepository.findByOrganizationIdAndSenderNameIgnoreCase(orgId, "UNKNOWN"))
                .thenReturn(Optional.empty());
        when(senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.resolveApprovedSender(orgId, "UNKNOWN"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Sender ID is not approved for this organization", ex.getMessage());
    }

    @Test
    void omittedSenderUsesTalkSasaConfiguredDefault() {
        assertEquals("TALK-SASA", service.resolveApprovedSender(orgId, null));
        assertEquals("TALK-SASA", service.resolveApprovedSender(orgId, "  "));
    }

    @Test
    void omittedSenderUsesTalkSasaConfigOverride() {
        properties.getSms().getTalksasa().setDefaultSenderId("MYTEST");
        assertEquals("MYTEST", service.resolveApprovedSender(orgId, null));
    }

    @Test
    void explicitTalkSasaDefaultIsAllowedWithoutOrgRow() {
        when(senderIdRepository.findByOrganizationIdAndSenderNameIgnoreCase(orgId, "TALK_SASA"))
                .thenReturn(Optional.empty());
        when(senderIdRepository.findByOrganizationIdAndSenderNameIgnoreCase(orgId, "TALK-SASA"))
                .thenReturn(Optional.empty());
        when(senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED))
                .thenReturn(Optional.empty());

        assertEquals("TALK-SASA", service.resolveApprovedSender(orgId, "TALK_SASA"));
        assertEquals("TALK-SASA", service.resolveApprovedSender(orgId, "TALK-SASA"));
    }

    @Test
    void africastalkingOmittedSenderUsesPlatformDefault() {
        properties.getSms().setProvider("africastalking");
        SenderId platform = SenderId.builder()
                .senderName("NOVASTACK")
                .status(SenderIdStatus.APPROVED)
                .platformDefault(true)
                .build();
        when(senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED))
                .thenReturn(Optional.of(platform));

        assertEquals("NOVASTACK", service.resolveApprovedSender(orgId, "  "));
    }
}
