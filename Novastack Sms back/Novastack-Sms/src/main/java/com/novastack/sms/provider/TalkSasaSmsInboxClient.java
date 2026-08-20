package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.TalkSasaSmsItemResponse;
import com.novastack.sms.dto.response.TalkSasaSmsListResponse;
import com.novastack.sms.dto.response.TalkSasaSmsViewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TalkSasaSmsInboxClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public TalkSasaSmsInboxClient(
            AppProperties appProperties,
            ObjectMapper objectMapper,
            @Qualifier("talkSasaWebClient") WebClient talkSasaWebClient) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.webClient = talkSasaWebClient;
    }

    public boolean isConfigured() {
        return apiToken() != null;
    }

    public TalkSasaSmsListResponse list(int page, int perPage) {
        int safePage = Math.max(1, page);
        int safePerPage = Math.min(100, Math.max(1, perPage));
        if (!isConfigured()) {
            return TalkSasaSmsListResponse.builder()
                    .configured(false)
                    .reachable(false)
                    .errorMessage(ProviderErrorMessages.NOT_CONFIGURED)
                    .page(safePage)
                    .perPage(safePerPage)
                    .items(List.of())
                    .build();
        }

        FetchResult result = getJson(uri -> uri
                .path("/sms")
                .queryParam("page", safePage)
                .queryParam("limit", safePerPage)
                .queryParam("per_page", safePerPage)
                .build());
        if (!result.ok()) {
            return TalkSasaSmsListResponse.builder()
                    .configured(true)
                    .reachable(false)
                    .errorMessage(result.error())
                    .page(safePage)
                    .perPage(safePerPage)
                    .items(List.of())
                    .build();
        }

        List<TalkSasaSmsItemResponse> items = parseItems(result.root(), result.data());
        Integer pageNumber = firstInt(result.root(), result.data(), "current_page", "page", "number");
        Integer pageSize = firstInt(result.root(), result.data(), "per_page", "limit", "size");
        return TalkSasaSmsListResponse.builder()
                .configured(true)
                .reachable(true)
                .page(pageNumber == null ? safePage : pageNumber)
                .perPage(pageSize == null ? safePerPage : pageSize)
                .total(firstLong(result.root(), result.data(), "total", "total_elements", "count"))
                .lastPage(firstInt(result.root(), result.data(), "last_page", "total_pages", "lastPage"))
                .items(items)
                .build();
    }

    public TalkSasaSmsViewResponse get(String uid) {
        if (!isConfigured()) {
            return TalkSasaSmsViewResponse.builder()
                    .configured(false)
                    .reachable(false)
                    .errorMessage(ProviderErrorMessages.NOT_CONFIGURED)
                    .build();
        }
        String id = uid == null ? "" : uid.trim();
        if (id.isBlank()) {
            return TalkSasaSmsViewResponse.builder()
                    .configured(true)
                    .reachable(true)
                    .errorMessage("TalkSasa SMS uid is required")
                    .build();
        }

        FetchResult result = getJson(uri -> uri.path(statusPath(id)).build(id));
        if (!result.ok()) {
            String fallback = "/sms/queue/{uid}".equals(statusPath(id)) ? "/sms/{uid}" : "/sms/queue/{uid}";
            FetchResult fallbackResult = getJson(uri -> uri.path(fallback).build(id));
            if (fallbackResult.ok()) {
                result = fallbackResult;
            } else {
                return TalkSasaSmsViewResponse.builder()
                        .configured(true)
                        .reachable(result.httpStatus() != 0 || fallbackResult.httpStatus() != 0)
                        .errorMessage(firstNonBlank(result.error(), fallbackResult.error()))
                        .build();
            }
        }

        TalkSasaSmsItemResponse item = parseItem(result.data());
        if (item != null && (item.getUid() == null || item.getUid().isBlank())) {
            item.setUid(id);
        }
        return TalkSasaSmsViewResponse.builder()
                .configured(true)
                .reachable(true)
                .item(item)
                .build();
    }

    private FetchResult getJson(java.util.function.Function<UriBuilder, java.net.URI> uriFunction) {
        try {
            String body = webClient.get()
                    .uri(uriFunction)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(readTimeout());
            JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa SMS inbox returned error");
                return FetchResult.fail(ProviderErrorMessages.VALIDATION, 200);
            }
            return FetchResult.ok(root, extractData(root));
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("TalkSasa SMS inbox HTTP error status={} body={}",
                    status, abbreviate(redact(ex.getResponseBodyAsString())));
            String message = status == 404 ? "TalkSasa SMS not found" : ProviderErrorMessages.forHttpStatus(status);
            return FetchResult.fail(message, status);
        } catch (WebClientRequestException ex) {
            log.error("TalkSasa SMS inbox connection failure message={}", safeMessage(ex));
            return FetchResult.fail(ProviderErrorMessages.UNAVAILABLE, 0);
        } catch (Exception ex) {
            log.error("TalkSasa SMS inbox failed: {}", safeMessage(ex));
            return FetchResult.fail(ProviderErrorMessages.UNAVAILABLE, 0);
        }
    }

    private List<TalkSasaSmsItemResponse> parseItems(JsonNode root, JsonNode data) {
        List<JsonNode> nodes = new ArrayList<>();
        collectArray(nodes, data);
        if (nodes.isEmpty()) {
            collectArray(nodes, root);
        }
        List<TalkSasaSmsItemResponse> items = new ArrayList<>();
        for (JsonNode node : nodes) {
            TalkSasaSmsItemResponse item = parseItem(node);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private void collectArray(List<JsonNode> out, JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(out::add);
            return;
        }
        if (!node.isObject()) {
            return;
        }
        for (String field : new String[] {"data", "sms", "messages", "items"}) {
            JsonNode nested = node.get(field);
            if (nested != null && nested.isArray()) {
                nested.forEach(out::add);
                return;
            }
            if (nested != null && nested.isObject()) {
                JsonNode inner = nested.get("data");
                if (inner != null && inner.isArray()) {
                    inner.forEach(out::add);
                    return;
                }
            }
        }
        if (firstText(node, "uid", "queue_uid", "id") != null) {
            out.add(node);
        }
    }

    private TalkSasaSmsItemResponse parseItem(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isObject()) {
            return null;
        }
        JsonNode nested = node.has("sms") && node.get("sms").isObject() ? node.get("sms") : node;
        String uid = firstText(nested, "uid", "queue_uid", "id", "message_id", "sms_uid");
        if (uid == null) {
            uid = uidFromStatusUrl(firstText(nested, "check_status_url", "status_url"));
        }
        Integer smsCount = firstInt(nested, null, "sms_count", "smsCount", "units", "sms_units");
        return TalkSasaSmsItemResponse.builder()
                .uid(uid)
                .recipient(firstText(nested, "to", "recipient", "phone", "number", "destination"))
                .senderId(firstText(nested, "from", "sender", "sender_id", "senderId", "source"))
                .message(firstText(nested, "message", "content", "sms", "text", "body"))
                .status(firstText(nested, "sms_status", "delivery_status", "status"))
                .type(firstText(nested, "type", "sms_type", "message_type"))
                .direction(firstText(nested, "direction", "sms_direction"))
                .cost(firstText(nested, "cost", "amount", "total_cost", "price"))
                .smsCount(smsCount)
                .createdAt(firstText(nested, "created_at", "createdAt", "sent_at", "sentAt", "date"))
                .build();
    }

    private JsonNode extractData(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.has("data") && !root.get("data").isNull()) {
            return root.get("data");
        }
        return root;
    }

    private boolean isErrorStatus(JsonNode root) {
        String status = firstText(root, "status");
        return status != null && ("error".equalsIgnoreCase(status)
                || "failed".equalsIgnoreCase(status)
                || "fail".equalsIgnoreCase(status));
    }

    private static String statusPath(String providerMessageId) {
        return looksLikeUuid(providerMessageId) ? "/sms/queue/{uid}" : "/sms/{uid}";
    }

    private static boolean looksLikeUuid(String value) {
        return value != null && value.length() == 36 && value.charAt(8) == '-';
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

    private String apiToken() {
        String token = appProperties.getSms().getTalksasa().getApiToken();
        return token == null || token.isBlank() ? null : token.trim();
    }

    private Duration readTimeout() {
        return Duration.ofMillis(Math.max(1_000, appProperties.getSms().getTalksasa().getReadTimeoutMs()));
    }

    private static Integer firstInt(JsonNode first, JsonNode second, String... fields) {
        Integer value = intValue(first, fields);
        return value != null ? value : intValue(second, fields);
    }

    private static Long firstLong(JsonNode first, JsonNode second, String... fields) {
        Long value = longValue(first, fields);
        return value != null ? value : longValue(second, fields);
    }

    private static Integer intValue(JsonNode node, String... fields) {
        String text = firstText(node, fields);
        if (text == null) {
            return null;
        }
        try {
            return new java.math.BigDecimal(text.replace(",", "").replaceAll("[^0-9.+-]", "")).intValue();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Long longValue(JsonNode node, String... fields) {
        String text = firstText(node, fields);
        if (text == null) {
            return null;
        }
        try {
            return new java.math.BigDecimal(text.replace(",", "").replaceAll("[^0-9.+-]", "")).longValue();
        } catch (Exception ex) {
            return null;
        }
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

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.replaceAll("(?i)(Bearer\\s+)\\S+", "$1***");
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 400 ? value.substring(0, 400) + "..." : value;
    }

    private static String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null) {
            return ex == null ? "unknown" : ex.getClass().getSimpleName();
        }
        return redact(ex.getMessage());
    }

    private record FetchResult(boolean ok, JsonNode root, JsonNode data, String error, int httpStatus) {
        static FetchResult ok(JsonNode root, JsonNode data) {
            return new FetchResult(true, root, data, null, 200);
        }

        static FetchResult fail(String error, int httpStatus) {
            return new FetchResult(false, null, null, error, httpStatus);
        }
    }
}
