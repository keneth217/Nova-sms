package com.novastack.sms.databundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Maps Safaricom Dynamic Offers / Daraja errors to clear customer-facing messages.
 * <p>
 * Covers documented response / result codes:
 * <ul>
 *   <li>200 / 1000 / 0 / 00 — success</li>
 *   <li>400 — unsuccessful request, invalid MSISDN, insufficient balance, technical error, bad headers</li>
 *   <li>401 / 401.001 — invalid access token</li>
 *   <li>500 — invalid parameter input</li>
 *   <li>503 — service under maintenance</li>
 * </ul>
 * Note: Safaricom often returns a generic CustomerMsg ("technical issue") while ResponseMsg
 * has the real reason (e.g. insufficient balance). Always inspect the full payload.
 */
public final class SafaricomApiErrorMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SafaricomApiErrorMapper() {
    }

    public static ApiException fromHttp(String action, WebClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        String businessCode = extractBusinessCode(body);
        String mapped = map(status, body, businessCode);
        return new ApiException(mapped, httpStatusFor(status, body, businessCode));
    }

    public static ApiException fromBody(String action, int httpOrBusinessStatus, String body, String businessCode) {
        String code = firstNonBlank(businessCode, extractBusinessCode(body));
        String mapped = map(httpOrBusinessStatus, body, code);
        return new ApiException(mapped, httpStatusFor(httpOrBusinessStatus, body, code));
    }

    public static ApiException fromThrowable(String action, Throwable ex) {
        if (ex instanceof ApiException api) {
            return api;
        }
        if (ex instanceof WebClientResponseException wcre) {
            return fromHttp(action, wcre);
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof TimeoutException || contains(ex.getMessage(), "timeout", "timed out")) {
            return new ApiException(
                    "Safaricom took too long to respond. Please try again in a moment.",
                    HttpStatus.GATEWAY_TIMEOUT);
        }
        if (ex instanceof WebClientRequestException || contains(ex.getMessage(), "connection", "network", "unreachable")) {
            return new ApiException(
                    "Could not reach Safaricom. Check your connection and try again.",
                    HttpStatus.BAD_GATEWAY);
        }
        return new ApiException(
                "Unable to complete " + action + " right now. Please try again later.",
                HttpStatus.BAD_GATEWAY);
    }

    public static void assertOffersSuccess(JsonNode root) {
        String status = firstNonBlank(
                text(root, "status", "Status", "responseStatus", "ResponseCode", "responseCode"),
                text(root, "errorCode", "ErrorCode", "resultCode", "ResultCode"));
        if (status == null) {
            return;
        }
        if (isSuccess(status)) {
            return;
        }
        throw fromBody("fetch offers", parseIntSafe(status, 400), root.toString(), status);
    }

    public static void assertPurchaseSuccess(JsonNode root) {
        JsonNode header = root.path("header");
        JsonNode node = header.isMissingNode() || header.isNull() ? root : header;
        String code = firstNonBlank(
                text(node, "responseCode", "ResponseCode", "status", "Status"),
                text(root, "errorCode", "ErrorCode", "resultCode", "ResultCode", "responseCode", "ResponseCode"));
        if (code == null) {
            // Some Daraja failures only return errorMessage with HTTP 200.
            String errorMessage = text(root, "errorMessage", "ErrorMessage");
            if (errorMessage != null && !errorMessage.isBlank()) {
                throw fromBody("purchase", 400, root.toString(), "400");
            }
            return;
        }
        if (isSuccess(code)) {
            return;
        }
        // Pass the full payload — CustomerMsg is often a generic "technical issue" placeholder.
        throw fromBody("purchase", parseIntSafe(code, 400), root.toString(), code);
    }

    public static String map(int status, String body, String businessCode) {
        String code = normalizeCode(businessCode);
        String hay = ((body == null ? "" : body) + " " + (code == null ? "" : code))
                .toLowerCase(Locale.ROOT);
        String detail = extractDetailedMessage(body);

        // --- Documented result / response codes (check codes before generic text) ---

        // 401 / 401.001 — Invalid Access Token
        if (status == 401 || "401".equals(code) || "401.001".equals(code)
                || hay.contains("401.001")
                || hay.contains("unauthorised")
                || hay.contains("unauthorized")
                || hay.contains("invalid access token")) {
            return "Safaricom rejected the access token for Dynamic Offers. "
                    + "Using the same Consumer Key/Secret as M-Pesa is fine, but that Daraja app "
                    + "must also have the Dynamic Offers / Mobile Data Bundles product enabled. "
                    + "Add the product in the Safaricom Developer Portal, then retry.";
        }

        // 503 — Service under maintenance
        if (status == 503 || "503".equals(code)
                || hay.contains("under maintenance")
                || hay.contains("service unavailable")
                || hay.contains("service is currently under maintenance")) {
            return "Safaricom data bundles are temporarily unavailable. Please try again later.";
        }

        // 500 — Invalid parameter input
        if ("500".equals(code) || hay.contains("invalid parameter")) {
            return "The bundle request had invalid parameters. Fetch offers again and retry.";
        }

        // Insufficient balance (often ResponseMsg; CustomerMsg is a generic technical placeholder)
        if (hay.contains("insufficient") || hay.contains("balance is not enough")
                || hay.contains("balance is insufficient")
                || (detail != null && detail.toLowerCase(Locale.ROOT).contains("insufficient"))) {
            return "Insufficient airtime or M-Pesa balance to buy this bundle. Top up and try again.";
        }

        // Invalid MSISDN
        if (hay.contains("invalid msisdn") || hay.contains("operation failed: invalid")) {
            return "That Safaricom number is invalid or not eligible. Use a valid 07… / 011… / 254… number.";
        }

        // Missing / wrong purchase headers (x-source-system, x-correlation-conversationid, x-msisdn, …)
        if (hay.contains("request header invalid") || hay.contains("invalid request header")
                || hay.contains("header invalid")) {
            return "Safaricom rejected the purchase headers. "
                    + "Required: Authorization, x-source-system, x-correlation-conversationid, x-msisdn, x-key-type.";
        }

        if (hay.contains("not eligible") || hay.contains("ineligible") || hay.contains("eligibility")) {
            return "This number is not eligible for data bundle offers right now.";
        }

        if (hay.contains("duplicate") || hay.contains("already being processed")
                || hay.contains("another instance")) {
            return "A purchase for this offer is already in progress. Wait a minute, then check status or retry.";
        }

        if (hay.contains("cancel") || hay.contains("request cancelled") || hay.contains("user cancelled")) {
            return "Payment was cancelled on the phone. You can try again when ready.";
        }

        if (hay.contains("timeout") || hay.contains("timed out") || hay.contains("did not complete")) {
            return "Payment timed out before it was completed. Check status or try again.";
        }

        if (hay.contains("offer") && (hay.contains("not found") || hay.contains("expired") || hay.contains("invalid"))) {
            return "That offer is no longer available. Fetch offers again and pick a current one.";
        }

        // Documented 400 "Technical Error" / generic technical CustomerMsg
        if (hay.contains("technical error") || hay.contains("technical issue")
                || hay.contains("experiencing a technical")) {
            if (detail != null && !isGenericTechnicalMessage(detail)) {
                return detail;
            }
            // When Safaricom returns the same placeholder for responseMessage and customerMessage,
            // the usual causes are payment channel / product config — not our JSON shape.
            return "Safaricom could not fulfil this bundle (technical error). "
                    + "Try payment mode Airtime first. For M-Pesa, confirm in the Daraja portal that "
                    + "Dynamic Offers has M-Pesa payment enabled and a live paybill/till is linked. "
                    + "Also check the line has enough airtime/M-Pesa balance.";
        }

        // 400 — Unsuccessfully request (generic documented result code)
        if (status == 400 || "400".equals(code)) {
            if (detail != null && !isGenericTechnicalMessage(detail)) {
                return detail;
            }
            return "Safaricom could not process this request. Check the number, offer, and balance, then try again.";
        }

        if (status >= 500) {
            return "Safaricom is experiencing a temporary problem. Please try again later.";
        }

        if (detail != null && !isGenericTechnicalMessage(detail)) {
            return detail;
        }
        if (detail != null) {
            return detail;
        }
        return "Safaricom request failed"
                + (code == null || code.isBlank() ? "" : " (code " + code + ")")
                + ". Please try again.";
    }

    private static HttpStatus httpStatusFor(int status, String body, String businessCode) {
        String code = normalizeCode(businessCode);
        String hay = ((body == null ? "" : body) + " " + (code == null ? "" : code))
                .toLowerCase(Locale.ROOT);

        if (status == 401 || "401".equals(code) || "401.001".equals(code)
                || hay.contains("unauthorized") || hay.contains("unauthorised") || hay.contains("401.001")) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (status == 503 || "503".equals(code) || hay.contains("under maintenance")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (status == 504 || hay.contains("timeout") || hay.contains("timed out")) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if ("500".equals(code) || status >= 500) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (status == 400 || "400".equals(code)
                || hay.contains("insufficient")
                || hay.contains("invalid")
                || hay.contains("header")) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Prefer ResponseMsg / responseMessage / errorMessage over CustomerMsg when the latter is generic.
     */
    private static String extractDetailedMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode header = root.path("header");

            String responseMsg = firstNonBlank(
                    text(root, "ResponseMsg", "responseMsg", "errorMessage", "ErrorMessage"),
                    text(header, "responseMessage", "ResponseMessage"),
                    text(root, "responseMessage", "ResponseMessage", "responseDesc", "ResponseDesc"));
            String customerMsg = firstNonBlank(
                    text(root, "CustomerMsg", "customerMsg", "CustomerMessage", "customerMessage"),
                    text(header, "customerMessage", "CustomerMessage"),
                    text(root, "desc", "message", "Message"));

            if (responseMsg != null && !isGenericTechnicalMessage(responseMsg)) {
                return responseMsg;
            }
            if (customerMsg != null && !isGenericTechnicalMessage(customerMsg)) {
                return customerMsg;
            }
            return firstNonBlank(responseMsg, customerMsg);
        } catch (Exception ignored) {
            // Plain-text Safaricom errors: ResponseMsg=... CustomerMsg=...
            String responseMsg = firstNonBlank(
                    extractLabeled(body, "ResponseMsg"),
                    extractLabeled(body, "errorMessage"),
                    extractLabeled(body, "ErrorMessage"));
            String customerMsg = extractLabeled(body, "CustomerMsg");
            if (responseMsg != null && !isGenericTechnicalMessage(responseMsg)) {
                return responseMsg;
            }
            if (customerMsg != null && !isGenericTechnicalMessage(customerMsg)) {
                return customerMsg;
            }
            // Whole body may be a short plain message like "Request Header Invalid"
            String trimmed = body.trim();
            if (!trimmed.startsWith("{") && trimmed.length() < 200) {
                return trimmed;
            }
            return firstNonBlank(responseMsg, customerMsg);
        }
    }

    private static String extractBusinessCode(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode header = root.path("header");
            return firstNonBlank(
                    text(header, "responseCode", "ResponseCode", "resultCode", "ResultCode"),
                    text(root, "errorCode", "ErrorCode", "resultCode", "ResultCode",
                            "responseCode", "ResponseCode", "responseStatus", "ResponseStatus"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractLabeled(String body, String label) {
        int idx = body.toLowerCase(Locale.ROOT).indexOf(label.toLowerCase(Locale.ROOT) + "=");
        if (idx < 0) {
            return null;
        }
        int start = idx + label.length() + 1;
        int end = body.indexOf('.', start);
        int end2 = body.toLowerCase(Locale.ROOT).indexOf(" customer", start);
        if (end2 > start && (end < 0 || end2 < end)) {
            end = end2;
        }
        if (end < 0) {
            end = body.length();
        }
        String value = body.substring(start, end).trim();
        return value.isBlank() ? null : value;
    }

    private static boolean isGenericTechnicalMessage(String message) {
        String hay = message.toLowerCase(Locale.ROOT);
        return hay.contains("technical issue")
                || hay.contains("technical error")
                || hay.contains("try again later")
                || hay.contains("experiencing a technical")
                || hay.contains("unsuccessfully request");
    }

    public static boolean isSuccess(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = normalizeCode(code);
        return "0".equals(normalized)
                || "00".equals(normalized)
                || "200".equals(normalized)
                || "1000".equals(normalized)
                || "success".equalsIgnoreCase(code.trim())
                || "ok".equalsIgnoreCase(code.trim());
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Keep dotted codes like 401.001
        if (trimmed.matches("\\d+(\\.\\d+)?")) {
            return trimmed;
        }
        return trimmed;
    }

    private static boolean contains(String value, String... needles) {
        if (value == null) {
            return false;
        }
        String hay = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (hay.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            String code = normalizeCode(value);
            if (code == null) {
                return fallback;
            }
            // 401.001 → 401 for HTTP-ish mapping
            int dot = code.indexOf('.');
            return Integer.parseInt(dot > 0 ? code.substring(0, dot) : code);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }
}
