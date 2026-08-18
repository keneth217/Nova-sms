package com.novastack.sms.provider;

import com.novastack.sms.exception.HumanReadableErrors;

import java.util.Locale;

public final class ProviderErrorMessages {

    public static final String UNAVAILABLE = "SMS provider is temporarily unavailable. Please try again.";
    public static final String TIMEOUT = "SMS provider did not respond in time. Please try again.";
    public static final String AUTH = "SMS provider authentication failed. Please contact support.";
    public static final String FORBIDDEN = "SMS provider rejected this request. Please contact support.";
    public static final String VALIDATION = "The SMS could not be sent. Check the recipient, sender ID, and message.";
    public static final String INVALID_SENDER =
            "Sender ID is not authorized with the SMS provider. Register TALK-SASA on the provider, or send with a sender ID that is already approved there.";
    public static final String INVALID_RECIPIENT = "Invalid recipient phone number.";
    public static final String PROVIDER_UNITS =
            "SMS provider has no remaining units. Please contact support.";
    public static final String RATE_LIMIT = "Too many SMS requests. Please try again shortly.";
    public static final String MALFORMED = "SMS provider returned an unexpected response. Please try again.";
    public static final String NOT_CONFIGURED = "SMS provider is not configured. Please contact support.";

    private ProviderErrorMessages() {
    }

    public static String forHttpStatus(Integer status) {
        if (status == null) {
            return UNAVAILABLE;
        }
        return switch (status) {
            case 401 -> AUTH;
            case 403 -> FORBIDDEN;
            case 408 -> TIMEOUT;
            case 422, 400 -> VALIDATION;
            case 429 -> RATE_LIMIT;
            case 502, 503, 504 -> UNAVAILABLE;
            default -> status >= 500 ? UNAVAILABLE : VALIDATION;
        };
    }

    public static String fromVendorDetail(String detail, Integer httpStatus) {
        String lower = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "insufficient", "not enough", "no credit", "no unit", "low balance", "out of credit")) {
            return PROVIDER_UNITS;
        }
        if (containsAny(lower, "unauthentic", "invalid token", "api token")
                && !containsAny(lower, "originator", "sender", "not authorized to send")) {
            return AUTH;
        }
        if (containsAny(lower, "too many", "rate limit", "throttl")) {
            return RATE_LIMIT;
        }
        String human = HumanReadableErrors.fromVendor(detail);
        if (human != null) {
            return human;
        }
        if (containsAny(lower,
                "originator",
                "sender id",
                "sender_id",
                "senderid",
                "invalid sender",
                "selected sender",
                "not authorized to send")) {
            return INVALID_SENDER;
        }
        if (containsAny(lower, "recipient", "phone", "msisdn", "invalid number", "mobile")) {
            return INVALID_RECIPIENT;
        }
        return forHttpStatus(httpStatus != null ? httpStatus : 400);
    }

    public static boolean isRetryable(Integer status, boolean networkFailure) {
        if (networkFailure && status == null) {
            return true;
        }
        if (status == null) {
            return false;
        }
        return status == 408 || status == 502 || status == 503 || status == 504;
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
