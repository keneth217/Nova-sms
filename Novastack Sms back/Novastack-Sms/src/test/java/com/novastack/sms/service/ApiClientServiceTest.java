package com.novastack.sms.service;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.ApiPermission;
import com.novastack.sms.domain.repository.ApiClientRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.request.CreateApiClientRequest;
import com.novastack.sms.dto.response.ApiClientCreatedResponse;
import com.novastack.sms.security.ApiKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiClientServiceTest {

    @Mock
    private ApiClientRepository apiClientRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private ApiClientService service;
    private UUID orgId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new ApiClientService(apiClientRepository, organizationRepository);
        orgId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("Mwalimu").build();
    }

    @Test
    void createReturnsPlaintextOnceAndStoresHash() {
        CreateApiClientRequest request = new CreateApiClientRequest();
        request.setName("Mwalimu Backend");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(apiClientRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Mwalimu Backend")).thenReturn(false);
        when(apiClientRepository.existsByClientCode(any())).thenReturn(false);
        when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> {
            ApiClient client = invocation.getArgument(0);
            client.setId(UUID.randomUUID());
            return client;
        });

        ApiClientCreatedResponse created = service.create(orgId, request);

        assertTrue(created.getApiKey().startsWith("nova_live_"));
        ArgumentCaptor<ApiClient> captor = ArgumentCaptor.forClass(ApiClient.class);
        verify(apiClientRepository).save(captor.capture());
        ApiClient saved = captor.getValue();
        assertEquals(ApiKeyHasher.sha256Hex(created.getApiKey()), saved.getApiKeyHash());
        assertNotEquals(created.getApiKey(), saved.getApiKeyHash());
        assertTrue(saved.getPermissions().contains(ApiPermission.SMS_SEND));
        assertEquals(ApiClientStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void revokeReplacesHashAndMarksRevoked() {
        UUID clientId = UUID.randomUUID();
        ApiClient client = ApiClient.builder()
                .id(clientId)
                .organization(organization)
                .name("Mwalimu Backend")
                .clientCode("MWALIMU")
                .apiKeyHash("old-hash")
                .apiKeyPrefix("nova_live_old")
                .status(ApiClientStatus.ACTIVE)
                .build();
        when(apiClientRepository.findByIdAndOrganizationId(clientId, orgId)).thenReturn(Optional.of(client));
        when(apiClientRepository.save(any(ApiClient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.revoke(orgId, clientId);

        assertEquals(ApiClientStatus.REVOKED, response.getStatus());
        assertNotEquals("old-hash", client.getApiKeyHash());
    }
}
