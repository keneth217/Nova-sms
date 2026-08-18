package com.novastack.sms.service;

import com.novastack.sms.domain.entity.ApiIdempotencyKey;
import com.novastack.sms.domain.repository.ApiIdempotencyKeyRepository;
import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ApiIdempotencyKeyRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository);
    }

    @Test
    void secondCallWithSameKeyReturnsStoredResource() {
        UUID clientId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String hash = IdempotencyService.hashPayload("send", "254712345678");
        when(repository.findByApiClientIdAndIdempotencyKey(clientId, "payment-1"))
                .thenReturn(Optional.of(ApiIdempotencyKey.builder()
                        .apiClientId(clientId)
                        .idempotencyKey("payment-1")
                        .requestHash(hash)
                        .resourceType(IdempotencyService.TYPE_SMS)
                        .resourceId(messageId)
                        .build()));

        AtomicInteger runs = new AtomicInteger();
        UUID result = service.replayOrRun(
                clientId,
                "payment-1",
                hash,
                IdempotencyService.TYPE_SMS,
                () -> {
                    runs.incrementAndGet();
                    return new IdempotencyService.ReplayResult<>(UUID.randomUUID(), UUID.randomUUID());
                },
                id -> id);

        assertEquals(messageId, result);
        assertEquals(0, runs.get());
        verify(repository, never()).save(any());
    }

    @Test
    void reusedKeyWithDifferentBodyConflicts() {
        UUID clientId = UUID.randomUUID();
        when(repository.findByApiClientIdAndIdempotencyKey(clientId, "payment-1"))
                .thenReturn(Optional.of(ApiIdempotencyKey.builder()
                        .apiClientId(clientId)
                        .idempotencyKey("payment-1")
                        .requestHash("aaa")
                        .resourceType(IdempotencyService.TYPE_SMS)
                        .resourceId(UUID.randomUUID())
                        .build()));

        ApiException ex = assertThrows(ApiException.class, () -> service.replayOrRun(
                clientId,
                "payment-1",
                "bbb",
                IdempotencyService.TYPE_SMS,
                () -> new IdempotencyService.ReplayResult<>("x", UUID.randomUUID()),
                id -> "y"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
