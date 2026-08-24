package com.novastack.sms.service;

import com.novastack.sms.domain.entity.ApiIdempotencyKey;
import com.novastack.sms.domain.repository.ApiIdempotencyKeyRepository;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.security.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String TYPE_SMS = "SMS";
    public static final String TYPE_BATCH = "BATCH";
    public static final String TYPE_STK = "STK";

    private final ApiIdempotencyKeyRepository repository;

    @Transactional(readOnly = true)
    public java.util.Optional<ApiIdempotencyKey> find(UUID apiClientId, String idempotencyKey) {
        if (apiClientId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return java.util.Optional.empty();
        }
        return repository.findByApiClientIdAndIdempotencyKey(apiClientId, idempotencyKey.trim());
    }

    public void assertCompatible(ApiIdempotencyKey existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ApiException(
                    "Idempotency-Key was reused with a different request body",
                    HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void store(UUID apiClientId, String idempotencyKey, String requestHash, String resourceType, UUID resourceId) {
        if (apiClientId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        if (repository.findByApiClientIdAndIdempotencyKey(apiClientId, idempotencyKey.trim()).isPresent()) {
            return;
        }
        repository.save(ApiIdempotencyKey.builder()
                .apiClientId(apiClientId)
                .idempotencyKey(idempotencyKey.trim())
                .requestHash(requestHash)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build());
    }

    public static String hashPayload(String... parts) {
        String joined = String.join("\n", parts == null ? new String[] {} : parts);
        return ApiKeyHasher.sha256Hex(joined);
    }

    public <T> T replayOrRun(
            UUID apiClientId,
            String idempotencyKey,
            String requestHash,
            String resourceType,
            Supplier<ReplayResult<T>> runner,
            java.util.function.Function<UUID, T> loader) {
        if (apiClientId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return runner.get().value();
        }
        var existing = find(apiClientId, idempotencyKey);
        if (existing.isPresent()) {
            assertCompatible(existing.get(), requestHash);
            return loader.apply(existing.get().getResourceId());
        }
        ReplayResult<T> result = runner.get();
        store(apiClientId, idempotencyKey, requestHash, resourceType, result.resourceId());
        return result.value();
    }

    public record ReplayResult<T>(T value, UUID resourceId) {
    }
}
