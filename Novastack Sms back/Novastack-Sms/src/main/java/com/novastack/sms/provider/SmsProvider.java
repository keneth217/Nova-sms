package com.novastack.sms.provider;

import com.novastack.sms.domain.enums.MessageStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SmsProvider {

    String getName();

    default boolean supportsStatusLookup() {
        return false;
    }

    SmsProviderResult send(SmsProviderRequest request);

    Map<String, SmsProviderResult> sendBulk(SmsBulkRequest request);

    default SmsStatusResult getSmsStatus(String providerMessageId) {
        return SmsStatusResult.unsupported(getName());
    }

    record SmsProviderRequest(
            String username,
            String apiKey,
            String recipient,
            String message,
            String senderId,
            String baseUrl,
            String type
    ) {
        public SmsProviderRequest(String recipient, String message, String senderId) {
            this(null, null, recipient, message, senderId, null, "plain");
        }

        public SmsProviderRequest(
                String username,
                String apiKey,
                String recipient,
                String message,
                String senderId,
                String baseUrl) {
            this(username, apiKey, recipient, message, senderId, baseUrl, "plain");
        }

        public String resolvedType() {
            return type == null || type.isBlank() ? "plain" : type;
        }
    }

    record SmsBulkRequest(
            String username,
            String apiKey,
            Collection<String> recipients,
            String message,
            String senderId,
            String baseUrl,
            String type
    ) {
        public SmsBulkRequest(Collection<String> recipients, String message, String senderId) {
            this(null, null, recipients, message, senderId, null, "plain");
        }

        public SmsBulkRequest(
                String username,
                String apiKey,
                Collection<String> recipients,
                String message,
                String senderId,
                String baseUrl) {
            this(username, apiKey, recipients, message, senderId, baseUrl, "plain");
        }

        public String resolvedType() {
            return type == null || type.isBlank() ? "plain" : type;
        }
    }

    record SmsProviderResult(
            boolean success,
            String providerMessageId,
            String rawRequest,
            String rawResponse,
            Integer httpStatus,
            String errorMessage,
            boolean retryable,
            MessageStatus mappedStatus
    ) {
        public static SmsProviderResult accepted(
                String providerMessageId,
                String rawRequest,
                String rawResponse,
                Integer httpStatus) {
            return accepted(providerMessageId, rawRequest, rawResponse, httpStatus, MessageStatus.ACCEPTED);
        }

        public static SmsProviderResult accepted(
                String providerMessageId,
                String rawRequest,
                String rawResponse,
                Integer httpStatus,
                MessageStatus mappedStatus) {
            return new SmsProviderResult(
                    true,
                    providerMessageId,
                    rawRequest,
                    rawResponse,
                    httpStatus,
                    null,
                    false,
                    mappedStatus != null ? mappedStatus : MessageStatus.ACCEPTED);
        }

        public static SmsProviderResult failure(String rawRequest, String rawResponse, Integer httpStatus, String error) {
            boolean retryable = ProviderErrorMessages.isRetryable(httpStatus, httpStatus == null);
            return failure(rawRequest, rawResponse, httpStatus, error, retryable);
        }

        public static SmsProviderResult failure(
                String rawRequest,
                String rawResponse,
                Integer httpStatus,
                String error,
                boolean retryable) {
            MessageStatus status = retryable ? MessageStatus.PENDING : MessageStatus.FAILED;
            if (httpStatus != null && (httpStatus == 422 || httpStatus == 400)) {
                status = MessageStatus.REJECTED;
            }
            return new SmsProviderResult(false, null, rawRequest, rawResponse, httpStatus, error, retryable, status);
        }

        public Optional<String> messageId() {
            return Optional.ofNullable(providerMessageId);
        }
    }

    record SmsStatusResult(
            boolean success,
            boolean supported,
            MessageStatus status,
            String providerStatus,
            String providerMessageId,
            String errorMessage,
            Integer httpStatus,
            Instant occurredAt,
            Integer processedCount,
            Integer failedCount,
            Integer remaining
    ) {
        public static SmsStatusResult unsupported(String providerName) {
            return new SmsStatusResult(
                    false,
                    false,
                    MessageStatus.PENDING,
                    null,
                    null,
                    providerName + " does not support status lookup",
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public static SmsStatusResult of(MessageStatus status, String providerStatus, String providerMessageId) {
            return of(status, providerStatus, providerMessageId, null, null, null, null);
        }

        public static SmsStatusResult of(
                MessageStatus status,
                String providerStatus,
                String providerMessageId,
                Instant occurredAt,
                Integer processedCount,
                Integer failedCount,
                Integer remaining) {
            return new SmsStatusResult(
                    true, true, status, providerStatus, providerMessageId, null, 200,
                    occurredAt, processedCount, failedCount, remaining);
        }

        public static SmsStatusResult skipped(String providerStatus, String providerMessageId) {
            return new SmsStatusResult(
                    false, true, null, providerStatus, providerMessageId,
                    "Ambiguous multi-recipient TalkSasa queue result", 200,
                    null, null, null, null);
        }

        public static SmsStatusResult failure(String error, Integer httpStatus) {
            return new SmsStatusResult(
                    false, true, MessageStatus.PENDING, null, null, error, httpStatus,
                    null, null, null, null);
        }
    }
}
