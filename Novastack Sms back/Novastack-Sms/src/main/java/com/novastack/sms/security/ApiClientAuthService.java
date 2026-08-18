package com.novastack.sms.security;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.repository.ApiClientRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiClientAuthService {

    private final ApiClientRepository apiClientRepository;
    private final OrganizationRepository organizationRepository;
    private final ApiClientRateLimiter apiClientRateLimiter;

    @Transactional
    public Optional<UserPrincipal> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String key = rawKey.trim();
        if (ApiKeyHasher.looksLikeNovaLiveKey(key)) {
            return authenticateClient(key);
        }
        return authenticateLegacyOrgKey(key);
    }

    private Optional<UserPrincipal> authenticateClient(String key) {
        String hash = ApiKeyHasher.sha256Hex(key);
        Optional<ApiClient> found = apiClientRepository.findByApiKeyHash(hash);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ApiClient client = found.get();
        if (!client.isUsable()) {
            return Optional.empty();
        }
        var org = client.getOrganization();
        if (org == null || org.getStatus() != OrganizationStatus.ACTIVE || org.isExpired()) {
            return Optional.empty();
        }
        try {
            apiClientRateLimiter.check(client.getId(), client.getRateLimitPerMinute());
        } catch (ApiException ex) {
            throw ex;
        }
        apiClientRepository.touchLastUsed(client.getId(), Instant.now());
        log.debug("API client authenticated prefix={} org={}", client.getApiKeyPrefix(), org.getId());
        return Optional.of(UserPrincipal.fromApiClient(client));
    }

    private Optional<UserPrincipal> authenticateLegacyOrgKey(String key) {
        return organizationRepository.findByApiKeyAndStatus(key, OrganizationStatus.ACTIVE)
                .filter(org -> !org.isExpired())
                .map(org -> UserPrincipal.fromLegacyApiKey(org.getId(), org.getName()));
    }
}
