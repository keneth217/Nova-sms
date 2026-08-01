package com.novastack.sms.databundle;

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
public class DataBundleRateLimiter {

    private final Map<UUID, Deque<Long>> buckets = new ConcurrentHashMap<>();

    public void check(UUID organizationId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        Deque<Long> hits = buckets.computeIfAbsent(organizationId, id -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < windowStart) {
                hits.removeFirst();
            }
            if (hits.size() >= limitPerMinute) {
                throw new ApiException(
                        "Too many data-bundle requests. Please wait a minute and try again.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            hits.addLast(now);
        }
    }
}
