package com.novastack.sms.provider;

import java.util.Optional;

public interface SmsProvider {

    String getName();

    SmsProviderResult send(SmsProviderRequest request);

    record SmsProviderRequest(
            String username,
            String apiKey,
            String recipient,
            String message,
            String senderId,
            String baseUrl
    ) {
    }

    record SmsProviderResult(
            boolean success,
            String providerMessageId,
            String rawRequest,
            String rawResponse,
            Integer httpStatus,
            String errorMessage
    ) {
        public static SmsProviderResult failure(String rawRequest, String rawResponse, Integer httpStatus, String error) {
            return new SmsProviderResult(false, null, rawRequest, rawResponse, httpStatus, error);
        }

        public Optional<String> messageId() {
            return Optional.ofNullable(providerMessageId);
        }
    }
}
