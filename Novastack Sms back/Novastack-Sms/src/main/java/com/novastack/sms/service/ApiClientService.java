package com.novastack.sms.service;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.ApiPermission;
import com.novastack.sms.domain.repository.ApiClientRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.request.CreateApiClientRequest;
import com.novastack.sms.dto.request.UpdateApiClientRequest;
import com.novastack.sms.dto.response.ApiClientCreatedResponse;
import com.novastack.sms.dto.response.ApiClientResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiClientService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiClientRepository apiClientRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public ApiClientCreatedResponse create(UUID actingOrganizationId, CreateApiClientRequest request) {
        UUID orgId = request.getOrganizationId() != null ? request.getOrganizationId() : actingOrganizationId;
        if (orgId == null) {
            throw new ApiException("Organization is required", HttpStatus.BAD_REQUEST);
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        String name = request.getName().trim();
        if (apiClientRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) {
            throw new ApiException("API client name already exists", HttpStatus.CONFLICT);
        }
        String plaintext = ApiKeyHasher.generateLiveKey();
        ApiClient client = ApiClient.builder()
                .organization(org)
                .name(name)
                .clientCode(uniqueClientCode(request.getClientCode(), name))
                .apiKeyHash(ApiKeyHasher.sha256Hex(plaintext))
                .apiKeyPrefix(ApiKeyHasher.prefixOf(plaintext))
                .status(ApiClientStatus.ACTIVE)
                .rateLimitPerMinute(request.getRateLimitPerMinute() != null ? request.getRateLimitPerMinute() : 100)
                .permissions(resolvePermissions(request.getPermissions()))
                .expiresAt(request.getExpiresAt())
                .build();
        client = apiClientRepository.save(client);
        return ApiClientCreatedResponse.builder()
                .client(toResponse(client))
                .apiKey(plaintext)
                .build();
    }

    @Transactional
    public ApiClientResponse update(UUID organizationId, UUID clientId, UpdateApiClientRequest request) {
        ApiClient client = requireClient(organizationId, clientId);
        if (client.getStatus() == ApiClientStatus.REVOKED) {
            throw new ApiException("Revoked API clients cannot be updated", HttpStatus.BAD_REQUEST);
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            String name = request.getName().trim();
            if (apiClientRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                    client.getOrganization().getId(), name, clientId)) {
                throw new ApiException("API client name already exists", HttpStatus.CONFLICT);
            }
            client.setName(name);
        }
        if (request.getPermissions() != null) {
            client.setPermissions(resolvePermissions(request.getPermissions()));
        }
        if (request.getRateLimitPerMinute() != null) {
            client.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getExpiresAt() != null) {
            client.setExpiresAt(request.getExpiresAt());
        }
        return toResponse(apiClientRepository.save(client));
    }

    @Transactional
    public ApiClientCreatedResponse rotate(UUID organizationId, UUID clientId) {
        ApiClient client = requireClient(organizationId, clientId);
        if (client.getStatus() == ApiClientStatus.REVOKED) {
            throw new ApiException("Revoked API clients cannot be rotated", HttpStatus.BAD_REQUEST);
        }
        String plaintext = ApiKeyHasher.generateLiveKey();
        client.setApiKeyHash(ApiKeyHasher.sha256Hex(plaintext));
        client.setApiKeyPrefix(ApiKeyHasher.prefixOf(plaintext));
        client.setStatus(ApiClientStatus.ACTIVE);
        client = apiClientRepository.save(client);
        return ApiClientCreatedResponse.builder()
                .client(toResponse(client))
                .apiKey(plaintext)
                .build();
    }

    @Transactional
    public ApiClientResponse setEnabled(UUID organizationId, UUID clientId, boolean enabled) {
        ApiClient client = requireClient(organizationId, clientId);
        if (client.getStatus() == ApiClientStatus.REVOKED) {
            throw new ApiException("Revoked API clients cannot be enabled", HttpStatus.BAD_REQUEST);
        }
        client.setStatus(enabled ? ApiClientStatus.ACTIVE : ApiClientStatus.DISABLED);
        return toResponse(apiClientRepository.save(client));
    }

    @Transactional
    public ApiClientResponse revoke(UUID organizationId, UUID clientId) {
        ApiClient client = requireClient(organizationId, clientId);
        String random = ApiKeyHasher.generateLiveKey();
        client.setApiKeyHash(ApiKeyHasher.sha256Hex(random));
        client.setApiKeyPrefix(ApiKeyHasher.prefixOf(random));
        client.setStatus(ApiClientStatus.REVOKED);
        return toResponse(apiClientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ApiClientResponse get(UUID organizationId, UUID clientId) {
        return toResponse(requireClient(organizationId, clientId));
    }

    @Transactional(readOnly = true)
    public java.util.List<ApiClientResponse> listForOrganization(UUID organizationId) {
        return apiClientRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ApiClientResponse> listAll(UUID organizationId, Pageable pageable) {
        Page<ApiClient> page = organizationId != null
                ? apiClientRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable)
                : apiClientRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::toResponse);
    }

    public ApiClientResponse toResponse(ApiClient client) {
        Organization org = client.getOrganization();
        return ApiClientResponse.builder()
                .id(client.getId())
                .organizationId(org != null ? org.getId() : null)
                .organizationName(org != null ? org.getName() : null)
                .name(client.getName())
                .clientCode(client.getClientCode())
                .apiKeyPrefix(client.getApiKeyPrefix())
                .status(client.getStatus())
                .permissions(client.getPermissions())
                .rateLimitPerMinute(client.getRateLimitPerMinute())
                .lastUsedAt(client.getLastUsedAt())
                .expiresAt(client.getExpiresAt())
                .createdAt(client.getCreatedAt())
                .build();
    }

    private ApiClient requireClient(UUID organizationId, UUID clientId) {
        if (organizationId == null) {
            return apiClientRepository.findById(clientId)
                    .orElseThrow(() -> new ApiException("API client not found", HttpStatus.NOT_FOUND));
        }
        return apiClientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ApiException("API client not found", HttpStatus.NOT_FOUND));
    }

    private Set<ApiPermission> resolvePermissions(Set<ApiPermission> requested) {
        if (requested == null || requested.isEmpty()) {
            return EnumSet.of(ApiPermission.SMS_SEND, ApiPermission.SMS_BULK, ApiPermission.SMS_STATUS);
        }
        return EnumSet.copyOf(requested);
    }

    private String uniqueClientCode(String requested, String name) {
        String base = requested != null && !requested.isBlank()
                ? slug(requested)
                : slug(name);
        if (base.isBlank()) {
            base = "CLIENT";
        }
        if (base.length() > 48) {
            base = base.substring(0, 48);
        }
        String code = base;
        int attempts = 0;
        while (apiClientRepository.existsByClientCode(code)) {
            code = base + "_" + Integer.toHexString(RANDOM.nextInt(0xFFFFF)).toUpperCase(Locale.ROOT);
            if (++attempts > 20) {
                throw new ApiException("Could not allocate a unique client code", HttpStatus.CONFLICT);
            }
        }
        return code;
    }

    private static String slug(String value) {
        String cleaned = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        cleaned = cleaned.replaceAll("^_+|_+$", "");
        return cleaned;
    }
}
