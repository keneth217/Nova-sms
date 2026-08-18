package com.novastack.sms.security;

import com.novastack.sms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiClientRateLimiter {

    private final Map<UUID, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void check(UUID apiClientId, int limitPerMinute) {
        if (apiClientId == null || limitPerMinute <= 0) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        Deque<Long> hits = buckets.computeIfAbsent(apiClientId, id -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < windowStart) {
                hits.removeFirst();
            }
            if (hits.size() >= limitPerMinute) {
                throw new ApiException(
                        "Too many API requests. Please wait a minute and try again.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            hits.addLast(now);
        }
    }
}
