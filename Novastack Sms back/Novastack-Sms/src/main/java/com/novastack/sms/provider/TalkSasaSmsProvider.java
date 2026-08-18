package com.novastack.sms.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class TalkSasaSmsProvider implements SmsProvider {

    public static final String PROVIDER_NAME = "TALKSASA";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public TalkSasaSmsProvider(
            AppProperties appProperties,
            ObjectMapper objectMapper,
            @Qualifier("talkSasaWebClient") WebClient talkSasaWebClient) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.webClient = talkSasaWebClient;
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supportsStatusLookup() {
        return true;
    }

    @Override
    public SmsProviderResult send(SmsProviderRequest request) {
        Map<String, SmsProviderResult> results = sendBulk(new SmsBulkRequest(
                request.username(),
                request.apiKey(),
                request.recipient() == null ? List.of() : List.of(request.recipient()),
                request.message(),
                request.senderId(),
                request.baseUrl(),
                request.resolvedType()
        ));
        return results.values().stream().findFirst()
                .orElse(SmsProviderResult.failure(null, null, null, ProviderErrorMessages.MALFORMED));
    }

    @Override
    public Map<String, SmsProviderResult> sendBulk(SmsBulkRequest request) {
        Map<String, SmsProviderResult> byRecipient = new LinkedHashMap<>();
        List<String> recipients = uniqueRecipients(request.recipients());
        if (recipients.isEmpty()) {
            return byRecipient;
        }

        String token = apiToken();
        if (token == null) {
            log.error("TalkSasa is not configured: TALKSASA_API_TOKEN is missing");
            return failAll(recipients, null, null, 401, ProviderErrorMessages.NOT_CONFIGURED, false);
        }

        String senderId = resolveSenderId(request.senderId());
        if (senderId == null || senderId.isBlank()) {
            return failAll(recipients, null, null, 400, "Sender ID is required", false);
        }
        if (senderId.length() > 11) {
            return failAll(recipients, null, null, 400, "Sender ID cannot exceed 11 characters", false);
        }

        SendBody body = new SendBody(
                String.join(",", recipients),
                senderId,
                request.resolvedType(),
                request.message());
        String rawRequest = toJsonSafe(body);
        log.info("TalkSasa SMS request type={} senderId={} recipients={} body={}",
                request.resolvedType(), senderId, recipients.size(), logBody(rawRequest));

        try {
            HttpExchange exchange = webClient.post()
                    .uri("/sms/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(body)
                    .exchangeToMono(this::readExchange)
                    .block(readTimeout());

            int status = exchange == null ? 0 : exchange.status();
            String responseBody = exchange == null ? "" : exchange.body();
            boolean successHttp = status >= 200 && status < 300;
            boolean envelopeError = successHttp && isErrorEnvelope(responseBody);
            log.info("TalkSasa SMS response outcome={} httpStatus={} type={} senderId={} recipients={} body={}",
                    successHttp && !envelopeError ? "SUCCESS" : "ERROR",
                    status,
                    request.resolvedType(),
                    senderId,
                    recipients.size(),
                    logBody(responseBody));

            if (!successHttp) {
                String customer = vendorCustomerMessage(status == 0 ? 502 : status, responseBody);
                boolean retryable = ProviderErrorMessages.isRetryable(status == 0 ? null : status, false);
                return failAll(recipients, rawRequest, responseBody, status == 0 ? null : status, customer, retryable);
            }
            return parseSendResponse(recipients, rawRequest, responseBody);
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String bodyText = ex.getResponseBodyAsString();
            log.error("TalkSasa SMS response outcome=ERROR httpStatus={} type={} senderId={} recipients={} body={}",
                    status, request.resolvedType(), senderId, recipients.size(), logBody(bodyText));
            String customer = vendorCustomerMessage(status, bodyText);
            boolean retryable = ProviderErrorMessages.isRetryable(status, false);
            return failAll(recipients, rawRequest, bodyText, status, customer, retryable);
        } catch (WebClientRequestException ex) {
            boolean timeout = isTimeout(ex);
            log.error("TalkSasa SMS response outcome=ERROR timeout={} type={} senderId={} recipients={} message={}",
                    timeout, request.resolvedType(), senderId, recipients.size(), safeMessage(ex));
            String customer = timeout ? ProviderErrorMessages.TIMEOUT : ProviderErrorMessages.UNAVAILABLE;
            return failAll(recipients, rawRequest, null, timeout ? 408 : null, customer, true);
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            boolean timeout = isTimeout(cause);
            log.error("TalkSasa SMS response outcome=ERROR timeout={} type={} senderId={} recipients={} message={}",
                    timeout, request.resolvedType(), senderId, recipients.size(), safeMessage(ex));
            String customer = timeout ? ProviderErrorMessages.TIMEOUT : ProviderErrorMessages.UNAVAILABLE;
            return failAll(recipients, rawRequest, null, timeout ? 408 : null, customer, timeout || isNetwork(cause));
        }
    }

    @Override
    public SmsStatusResult getSmsStatus(String providerMessageId) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return SmsStatusResult.failure("Provider message id is required", 400);
        }
        String token = apiToken();
        if (token == null) {
            log.error("TalkSasa is not configured: TALKSASA_API_TOKEN is missing");
            return SmsStatusResult.failure(ProviderErrorMessages.NOT_CONFIGURED, 401);
        }

        try {
            String id = providerMessageId.trim();
            HttpExchange exchange = fetchStatus(token, statusPath(id), id);
            if (!usableStatusResponse(exchange)) {
                String fallbackPath = "/sms/queue/{uid}".equals(statusPath(id)) ? "/sms/{uid}" : "/sms/queue/{uid}";
                HttpExchange fallback = fetchStatus(token, fallbackPath, id);
                if (usableStatusResponse(fallback)) {
                    exchange = fallback;
                }
            }

            int status = exchange == null ? 0 : exchange.status();
            String responseBody = exchange == null ? "" : exchange.body();
            boolean successHttp = status >= 200 && status < 300;
            log.info("TalkSasa status response outcome={} httpStatus={} uidSuffix={} body={}",
                    successHttp ? "SUCCESS" : "ERROR",
                    status,
                    suffix(providerMessageId),
                    logBody(responseBody));
            if (!successHttp) {
                return SmsStatusResult.failure(
                        ProviderErrorMessages.forHttpStatus(status == 0 ? 502 : status),
                        status == 0 ? null : status);
            }

            JsonNode root = objectMapper.readTree(responseBody.isBlank() ? "{}" : responseBody);
            JsonNode data = extractDataNode(root);
            if (isErrorStatus(root)) {
                String customer = vendorCustomerMessage(200, responseBody);
                return SmsStatusResult.failure(customer, 200);
            }

            String providerStatus = firstText(data, "status", "sms_status", "delivery_status");
            if (providerStatus == null || isSuccessWord(providerStatus)) {
                String nested = firstText(data, "sms_status", "delivery_status");
                if (nested != null) {
                    providerStatus = nested;
                } else {
                    providerStatus = firstText(root, "sms_status", "delivery_status");
                }
            }
            Integer failed = intValue(data, "failed_count", "failedCount");
            Integer remaining = intValue(data, "remaining");
            Integer processed = intValue(data, "processed_count", "processedCount");
            Integer recipients = intValue(data, "recipient_count", "recipients_count", "recipientCount");
            String error = firstText(data, "error", "failure_reason", "error_message");
            MessageStatus mapped = TalkSasaStatusMapper.fromQueue(
                    providerStatus, failed, remaining, processed, recipients, error);
            String uid = firstNonBlank(findUid(data), findUid(root));
            if (uid == null) {
                uid = providerMessageId;
            }
            log.info("TalkSasa status uidSuffix={} providerStatus={} mapped={} processed={} failed={} remaining={}",
                    suffix(uid), providerStatus, mapped, processed, failed, remaining);
            if (mapped == null) {
                return SmsStatusResult.skipped(providerStatus, uid);
            }
            Instant completedAt = parseInstant(firstText(data, "completed_at", "completedAt", "delivered_at", "deliveredAt"));
            return SmsStatusResult.of(mapped, providerStatus, uid, completedAt, processed, failed, remaining);
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("TalkSasa status response outcome=ERROR httpStatus={} uidSuffix={} body={}",
                    status, suffix(providerMessageId), logBody(ex.getResponseBodyAsString()));
            return SmsStatusResult.failure(ProviderErrorMessages.forHttpStatus(status), status);
        } catch (Exception ex) {
            log.error("TalkSasa status lookup failed uidSuffix={} message={}",
                    suffix(providerMessageId), safeMessage(ex));
            return SmsStatusResult.failure(ProviderErrorMessages.UNAVAILABLE, null);
        }
    }

    private static final String[] UID_FIELDS = {
            "uid", "queue_uid", "id", "message_id", "messageid", "messageId",
            "sms_uid", "sms_id", "smsid", "campaign_uid"
    };

    private Map<String, SmsProviderResult> parseSendResponse(
            List<String> requested,
            String rawRequest,
            String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody == null || responseBody.isBlank() ? "{}" : responseBody);
            if (isErrorStatus(root)) {
                String customer = vendorCustomerMessage(422, responseBody);
                return failAll(requested, rawRequest, responseBody, 422, customer, false);
            }

            Map<String, SmsProviderResult> byRecipient = new LinkedHashMap<>();
            List<JsonNode> items = extractReportItems(root);
            String envelopeUid = findUid(root);
            MessageStatus envelopeStatus = sendMappedStatus(firstText(root, "status"));

            if (items.isEmpty()) {
                if (envelopeUid == null) {
                    log.warn("TalkSasa send succeeded without uid body={}", abbreviate(redact(responseBody)));
                }
                return acceptAll(requested, envelopeUid, envelopeStatus, rawRequest, responseBody, byRecipient);
            }

            SmsProviderResult shared = null;
            for (JsonNode item : items) {
                String key = resultKey(firstText(item, "recipient", "number", "phone", "to"));
                String uid = firstNonBlank(findUid(item), envelopeUid);
                MessageStatus mapped = sendMappedStatus(firstText(item, "status", "sms_status"));
                SmsProviderResult result = mapped.isBillableFailure()
                        ? SmsProviderResult.failure(rawRequest, responseBody, 200, customerItemError(item), false)
                        : toSendResult(item, uid, rawRequest, responseBody, mapped);
                if (uid == null && result.success()) {
                    log.warn("TalkSasa send item succeeded without uid body={}", abbreviate(redact(responseBody)));
                }
                if (!key.isBlank()) {
                    byRecipient.put(key, result);
                }
                if (shared == null) {
                    shared = result;
                }
            }

            if (shared == null || (shared.success() && shared.providerMessageId() == null && envelopeUid != null)) {
                shared = toSendResult(items.getFirst(), envelopeUid, rawRequest, responseBody, envelopeStatus);
            }
            return fillMissing(requested, byRecipient, shared, rawRequest, responseBody, envelopeUid, envelopeStatus);
        } catch (Exception ex) {
            log.warn("TalkSasa send HTTP 200 with unparsed body, treating as accepted: {} body={}",
                    safeMessage(ex), abbreviate(redact(responseBody)));
            return acceptAll(requested, null, MessageStatus.ACCEPTED, rawRequest, responseBody, new LinkedHashMap<>());
        }
    }

    private Map<String, SmsProviderResult> fillMissing(
            List<String> requested,
            Map<String, SmsProviderResult> byRecipient,
            SmsProviderResult shared,
            String rawRequest,
            String responseBody,
            String envelopeUid,
            MessageStatus envelopeStatus) {
        SmsProviderResult fallback = shared != null
                ? shared
                : SmsProviderResult.accepted(
                        envelopeUid,
                        rawRequest,
                        responseBody,
                        200,
                        envelopeStatus == MessageStatus.PENDING ? MessageStatus.ACCEPTED : envelopeStatus);
        for (String recipient : requested) {
            String key = resultKey(recipient);
            if (!byRecipient.containsKey(key)) {
                byRecipient.put(key, fallback);
            }
        }
        return byRecipient;
    }

    private String resultKey(String number) {
        if (number == null || number.isBlank()) {
            return "";
        }
        return PhoneNormalizer.lookupKey(PhoneNormalizer.normalize(number.trim()));
    }

    private Map<String, SmsProviderResult> acceptAll(
            List<String> requested,
            String uid,
            MessageStatus mapped,
            String rawRequest,
            String responseBody,
            Map<String, SmsProviderResult> byRecipient) {
        SmsProviderResult accepted = SmsProviderResult.accepted(
                uid, rawRequest, responseBody, 200, mapped == MessageStatus.PENDING ? MessageStatus.ACCEPTED : mapped);
        for (String recipient : requested) {
            byRecipient.put(PhoneNormalizer.lookupKey(recipient), accepted);
        }
        return byRecipient;
    }

    private SmsProviderResult toSendResult(
            JsonNode item,
            String uid,
            String rawRequest,
            String responseBody,
            MessageStatus mapped) {
        if (mapped.isBillableFailure()) {
            return SmsProviderResult.failure(rawRequest, responseBody, 200, customerItemError(item), false);
        }
        MessageStatus status = mapped == MessageStatus.PENDING ? MessageStatus.ACCEPTED : mapped;
        return SmsProviderResult.accepted(uid, rawRequest, responseBody, 200, status);
    }

    private MessageStatus sendMappedStatus(String providerStatus) {
        if (providerStatus != null && isSuccessWord(providerStatus)) {
            return MessageStatus.ACCEPTED;
        }
        return TalkSasaStatusMapper.toInternal(providerStatus);
    }

    private String customerItemError(JsonNode item) {
        String error = firstText(item, "error", "message", "failure_reason");
        return ProviderErrorMessages.fromVendorDetail(error, 200);
    }

    private String vendorCustomerMessage(Integer httpStatus, String body) {
        return ProviderErrorMessages.fromVendorDetail(extractVendorDetail(body), httpStatus);
    }

    private String extractVendorDetail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            StringBuilder detail = new StringBuilder();
            appendDetail(detail, firstText(root, "message", "error", "error_message"));
            JsonNode data = extractDataNode(root);
            if (data != null && data != root) {
                appendDetail(detail, firstText(data, "message", "error", "failure_reason"));
            }
            JsonNode errors = root.get("errors");
            if (errors != null && errors.isObject()) {
                errors.fields().forEachRemaining(field -> {
                    appendDetail(detail, field.getKey().replace('_', ' '));
                    if (field.getValue().isArray()) {
                        field.getValue().forEach(node -> appendDetail(detail, node.asText("")));
                    } else {
                        appendDetail(detail, field.getValue().asText(""));
                    }
                });
            }
            return detail.toString();
        } catch (Exception ex) {
            return body;
        }
    }

    private static void appendDetail(StringBuilder target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(' ');
        }
        target.append(value.trim());
    }

    private List<JsonNode> extractReportItems(JsonNode root) {
        List<JsonNode> items = new ArrayList<>();
        JsonNode data = extractDataNode(root);
        if (data == null) {
            return items;
        }
        if (data.isArray()) {
            data.forEach(items::add);
            return items;
        }
        if (data.isTextual()) {
            return items;
        }
        if (data.isObject()) {
            for (String nestedName : List.of("sms", "messages", "reports", "data")) {
                JsonNode nested = data.path(nestedName);
                if (nested.isArray() && nested.size() > 0) {
                    nested.forEach(items::add);
                    return items;
                }
                if (nested.isObject() && !nested.isEmpty() && findUid(nested) != null) {
                    items.add(nested);
                    return items;
                }
            }
            items.add(data);
        }
        return items;
    }

    private JsonNode extractDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (!root.has("data") || root.get("data").isNull()) {
            return root;
        }
        return coerceJsonNode(root.get("data"));
    }

    private JsonNode coerceJsonNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return node;
        }
        if (!node.isTextual()) {
            return node;
        }
        String text = node.asText().trim();
        if (text.startsWith("{") || text.startsWith("[")) {
            try {
                return objectMapper.readTree(text);
            } catch (Exception ignored) {
                return node;
            }
        }
        return node;
    }

    private boolean isErrorEnvelope(String body) {
        try {
            JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            return isErrorStatus(root);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isErrorStatus(JsonNode root) {
        String status = firstText(root, "status");
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return "error".equals(normalized) || "failed".equals(normalized) || "fail".equals(normalized);
    }

    private static boolean isSuccessWord(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return "success".equals(normalized)
                || "ok".equals(normalized)
                || "successful".equals(normalized);
    }

    private String findUid(JsonNode node) {
        String uid = firstText(node, UID_FIELDS);
        if (uid != null) {
            return uid;
        }
        uid = uidFromStatusUrl(firstText(node, "check_status_url", "status_url"));
        if (uid != null) {
            return uid;
        }
        JsonNode data = extractDataNode(node);
        if (data != null && data != node) {
            uid = firstText(data, UID_FIELDS);
            if (uid != null) {
                return uid;
            }
            uid = uidFromStatusUrl(firstText(data, "check_status_url", "status_url"));
            if (uid != null) {
                return uid;
            }
            JsonNode nested = data.path("sms");
            if (nested.isObject()) {
                uid = firstText(nested, UID_FIELDS);
                if (uid != null) {
                    return uid;
                }
                return uidFromStatusUrl(firstText(nested, "check_status_url", "status_url"));
            }
        }
        return null;
    }

    private static String uidFromStatusUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        if (slash < 0 || slash >= url.length() - 1) {
            return null;
        }
        String last = url.substring(slash + 1).trim();
        int query = last.indexOf('?');
        if (query >= 0) {
            last = last.substring(0, query);
        }
        return last.isBlank() ? null : last;
    }

    private static String statusPath(String providerMessageId) {
        return looksLikeUuid(providerMessageId) ? "/sms/queue/{uid}" : "/sms/{uid}";
    }

    private static boolean looksLikeUuid(String value) {
        return value != null && value.length() == 36 && value.charAt(8) == '-';
    }

    private HttpExchange fetchStatus(String token, String path, String uid) {
        return webClient.get()
                .uri(path, uid)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchangeToMono(this::readExchange)
                .block(readTimeout());
    }

    private boolean usableStatusResponse(HttpExchange exchange) {
        if (exchange == null || exchange.status() < 200 || exchange.status() >= 300) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(exchange.body() == null || exchange.body().isBlank() ? "{}" : exchange.body());
            return !isErrorStatus(root);
        } catch (Exception ex) {
            return true;
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private Map<String, SmsProviderResult> failAll(
            Collection<String> recipients,
            String rawRequest,
            String rawResponse,
            Integer httpStatus,
            String error,
            boolean retryable) {
        Map<String, SmsProviderResult> byRecipient = new LinkedHashMap<>();
        SmsProviderResult failure = SmsProviderResult.failure(rawRequest, rawResponse, httpStatus, error, retryable);
        for (String recipient : recipients) {
            byRecipient.put(PhoneNormalizer.lookupKey(recipient), failure);
        }
        return byRecipient;
    }

    private List<String> uniqueRecipients(Collection<String> recipients) {
        if (recipients == null) {
            return List.of();
        }
        return recipients.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String resolveSenderId(String senderId) {
        String resolved = senderId != null && !senderId.isBlank()
                ? senderId.trim()
                : appProperties.getSms().getTalksasa().resolvedDefaultSenderId();
        if (resolved != null && "TALK_SASA".equalsIgnoreCase(resolved)) {
            return AppProperties.TalkSasa.DEFAULT_SENDER_ID;
        }
        return resolved;
    }

    private String apiToken() {
        String token = appProperties.getSms().getTalksasa().getApiToken();
        return token == null || token.isBlank() ? null : token.trim();
    }

    private Duration readTimeout() {
        return Duration.ofMillis(Math.max(1_000, appProperties.getSms().getTalksasa().getReadTimeoutMs()));
    }

    private String toJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value.trim()).toInstant();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static Integer intValue(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isNumber()) {
                return value.intValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    return Integer.parseInt(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private static boolean isTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof java.util.concurrent.TimeoutException
                    || (current.getClass().getName().contains("ReadTimeout"))
                    || (current.getMessage() != null && current.getMessage().toLowerCase().contains("timed out"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isNetwork(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof java.io.IOException || current instanceof WebClientRequestException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null) {
            return ex == null ? "unknown" : ex.getClass().getSimpleName();
        }
        return redact(ex.getMessage());
    }

    static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value
                .replaceAll("(?i)(Bearer\\s+)\\S+", "$1***")
                .replaceAll("(?i)(api[_-]?token\"?\\s*[:=]\\s*\")[^\"]+", "$1***")
                .replaceAll("(?i)(Authorization\"?\\s*[:=]\\s*\")[^\"]+", "$1***");
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 400 ? value.substring(0, 400) + "…" : value;
    }

    private static String suffix(String value) {
        if (value == null || value.length() < 6) {
            return value;
        }
        return "..." + value.substring(value.length() - 6);
    }

    private Mono<HttpExchange> readExchange(ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new HttpExchange(status, body));
    }

    private static String logBody(String value) {
        String redacted = redact(value);
        if (redacted == null || redacted.isBlank()) {
            return "(empty)";
        }
        return redacted.length() > 8_000 ? redacted.substring(0, 8_000) + "…" : redacted;
    }

    private record HttpExchange(int status, String body) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SendBody(
            String recipient,
            @JsonProperty("sender_id") String senderId,
            String type,
            String message
    ) {
    }
}
