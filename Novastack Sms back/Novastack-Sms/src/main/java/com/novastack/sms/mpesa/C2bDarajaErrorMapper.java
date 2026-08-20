package com.novastack.sms.mpesa;

import java.util.Locale;

/**
 * Maps Daraja C2B v2 register-url HTTP/error payloads to an operator-facing message.
 */
public final class C2bDarajaErrorMapper {

    private C2bDarajaErrorMapper() {
    }

    public static String message(int httpStatus, String body) {
        String blob = body == null ? "" : body;
        String lower = blob.toLowerCase(Locale.ROOT);

        if (alreadyRegistered(blob)) {
            return "C2B URLs are already registered for this shortcode. "
                    + "Delete them in Daraja URL management (self-service), then register again. "
                    + "Production registerurl is a one-time call.";
        }
        if (lower.contains("duplicate notification") || lower.contains("aggregator")
                || lower.contains("sp id is")) {
            return "URLs are registered on the aggregator/VPN (Broker) platform, so Daraja cannot accept them. "
                    + "Ask Safaricom to delete the aggregator URLs, then register on Daraja.";
        }
        if (contains(blob, "400.003.01") || lower.contains("invalid access token")) {
            return "M-Pesa access token is invalid or expired. Retry; Nova will request a new token.";
        }
        if (contains(blob, "400.003.02") || lower.contains("bad request")) {
            return "Daraja rejected the register-url request (missing or invalid fields). "
                    + "Check ShortCode, ResponseType (Completed or Cancelled), and HTTPS callback URLs.";
        }
        if (contains(blob, "400.002.05") || lower.contains("invalid request payload")) {
            return "Register-url JSON is invalid. Required: ShortCode, ResponseType, ConfirmationURL, ValidationURL.";
        }
        if (contains(blob, "500.003.03") || lower.contains("quota violation")) {
            return "Too many Daraja requests (quota). Wait and retry one registerurl call at a time.";
        }
        if (contains(blob, "500.003.02") || lower.contains("spike arrest")) {
            return "Daraja spike-arrest: the callback URLs were erroring. "
                    + "Fix the confirmation/validation endpoints so they always return HTTP 200 JSON, then retry.";
        }
        if (contains(blob, "404.003.01") || lower.contains("resource not found")) {
            return "Wrong Daraja path. Production registerurl is POST /mpesa/c2b/v2/registerurl.";
        }
        if (contains(blob, "404.001.04") || lower.contains("invalid authenticator header")) {
            return "Wrong HTTP method or auth header. Register URL is POST with Bearer token; OAuth is GET.";
        }
        if (lower.contains("m-pesa") || lower.contains("mpesa") || lower.contains("safaricom")
                || lower.contains("ngrok") || lower.contains("mockbin") || lower.contains("requestbin")) {
            return "Callback URL was rejected. Use HTTPS. Do not include keywords like mpesa, safaricom, exe, sql, "
                    + "or query, and do not use ngrok/mockbin/requestbin in production.";
        }
        if (httpStatus == 500 && blob.isBlank()) {
            return "Daraja returned HTTP 500 with an empty body. Confirm the app has the c2bv2 product and the shortcode is live.";
        }
        if (!blob.isBlank()) {
            return "C2B URL registration failed (HTTP " + httpStatus + "): " + blob;
        }
        return "C2B URL registration failed (HTTP " + httpStatus + ").";
    }

    public static boolean alreadyRegistered(String body) {
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("already registered") || lower.contains("urls are already registered");
    }

    public static boolean invalidAccessToken(String body) {
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("400.003.01") || lower.contains("invalid access token");
    }

    private static boolean contains(String body, String token) {
        return body != null && body.contains(token);
    }
}
