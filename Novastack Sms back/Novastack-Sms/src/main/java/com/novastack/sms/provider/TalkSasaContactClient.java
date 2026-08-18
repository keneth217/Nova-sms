package com.novastack.sms.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TalkSasaContactClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public TalkSasaContactClient(
            AppProperties appProperties,
            ObjectMapper objectMapper,
            @Qualifier("talkSasaWebClient") WebClient talkSasaWebClient) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.webClient = talkSasaWebClient;
    }

    public boolean isEnabled() {
        return appProperties.getSms().getTalksasa().isSyncContactGroups() && apiToken() != null;
    }

    public Optional<TalkSasaContact> store(String groupUid, String phone, String firstName, String lastName) {
        if (!isEnabled() || blank(groupUid) || blank(phone)) {
            return Optional.empty();
        }
        return mutate(
                HttpMethod.POST,
                "/contacts/" + encode(groupUid) + "/store",
                new ContactBody(phone, blankToNull(firstName), blankToNull(lastName)))
                .asContact();
    }

    public Optional<TalkSasaContact> get(String groupUid, String contactUid) {
        if (!isEnabled() || blank(groupUid) || blank(contactUid)) {
            return Optional.empty();
        }
        return mutate(
                HttpMethod.POST,
                "/contacts/" + encode(groupUid) + "/search/" + encode(contactUid),
                null)
                .asContact();
    }

    public Optional<TalkSasaContact> update(
            String groupUid, String contactUid, String phone, String firstName, String lastName) {
        if (!isEnabled() || blank(groupUid) || blank(contactUid) || blank(phone)) {
            return Optional.empty();
        }
        return mutate(
                HttpMethod.PATCH,
                "/contacts/" + encode(groupUid) + "/update/" + encode(contactUid),
                new ContactBody(phone, blankToNull(firstName), blankToNull(lastName)))
                .asContact();
    }

    public boolean delete(String groupUid, String contactUid) {
        if (!isEnabled() || blank(groupUid) || blank(contactUid)) {
            return false;
        }
        return mutate(
                HttpMethod.DELETE,
                "/contacts/" + encode(groupUid) + "/delete/" + encode(contactUid),
                null)
                .success();
    }

    public List<TalkSasaContact> list(String groupUid) {
        if (!isEnabled() || blank(groupUid)) {
            return List.of();
        }
        try {
            String body = exchange(HttpMethod.POST, "/contacts/" + encode(groupUid) + "/all", null);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa list contacts returned error");
                return List.of();
            }
            return parseContactList(root);
        } catch (Exception ex) {
            log.warn("TalkSasa list contacts failed: {}", safeMessage(ex));
            return List.of();
        }
    }

    private CallResult mutate(HttpMethod method, String path, Object payload) {
        try {
            String body = exchange(method, path, payload);
            JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa contact {} {} returned error", method, path);
                return CallResult.failure();
            }
            return new CallResult(true, parseContact(root).orElse(null));
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("TalkSasa contact HTTP error method={} path={} status={} body={}",
                    method, path, status, abbreviate(redact(ex.getResponseBodyAsString())));
            return CallResult.failure();
        } catch (WebClientRequestException ex) {
            log.error("TalkSasa contact connection failure method={} path={} message={}",
                    method, path, safeMessage(ex));
            return CallResult.failure();
        } catch (Exception ex) {
            log.error("TalkSasa contact {} {} failed: {}", method, path, safeMessage(ex));
            return CallResult.failure();
        }
    }

    private String exchange(HttpMethod method, String path, Object payload) {
        String token = apiToken();
        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        WebClient.RequestHeadersSpec<?> request = payload == null
                ? spec
                : spec.contentType(MediaType.APPLICATION_JSON).bodyValue(payload);
        return request.retrieve().bodyToMono(String.class).block(readTimeout());
    }

    private Optional<TalkSasaContact> parseContact(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null || data.isNull() || data.isMissingNode() || "null".equalsIgnoreCase(data.asText(""))) {
            return Optional.empty();
        }
        if (data.isArray() && data.size() > 0) {
            data = data.get(0);
        }
        return parseContactItem(data, root);
    }

    private List<TalkSasaContact> parseContactList(JsonNode root) {
        List<TalkSasaContact> contacts = new ArrayList<>();
        JsonNode data = extractData(root);
        if (data == null) {
            return contacts;
        }
        if (data.isArray()) {
            data.forEach(item -> parseContactItem(item, item).ifPresent(contacts::add));
            return contacts;
        }
        JsonNode nested = data.path("data");
        if (nested.isArray()) {
            nested.forEach(item -> parseContactItem(item, item).ifPresent(contacts::add));
            return contacts;
        }
        parseContact(root).ifPresent(contacts::add);
        return contacts;
    }

    private Optional<TalkSasaContact> parseContactItem(JsonNode item, JsonNode fallback) {
        if (item == null || !item.isObject()) {
            return Optional.empty();
        }
        String uid = firstText(item, "uid", "id", "contact_uid", "contact_id");
        if (uid == null || uid.isBlank()) {
            uid = firstText(fallback, "uid", "id", "contact_uid", "contact_id");
        }
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        String phone = firstText(item, "phone", "phone_number", "mobile");
        String firstName = firstText(item, "first_name", "firstName");
        String lastName = firstText(item, "last_name", "lastName");
        return Optional.of(new TalkSasaContact(uid, phone, firstName, lastName));
    }

    private JsonNode extractData(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.has("data")) {
            return root.get("data");
        }
        return root;
    }

    private boolean isErrorStatus(JsonNode root) {
        String status = firstText(root, "status");
        return status != null && ("error".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status));
    }

    private String apiToken() {
        String token = appProperties.getSms().getTalksasa().getApiToken();
        return token == null || token.isBlank() ? null : token.trim();
    }

    private Duration readTimeout() {
        return Duration.ofMillis(Math.max(1_000, appProperties.getSms().getTalksasa().getReadTimeoutMs()));
    }

    private static String encode(String value) {
        return UriUtils.encodePathSegment(value.trim(), StandardCharsets.UTF_8);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value.trim();
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

    public record TalkSasaContact(String uid, String phone, String firstName, String lastName) {
    }

    private record CallResult(boolean success, TalkSasaContact contact) {
        static CallResult failure() {
            return new CallResult(false, null);
        }

        Optional<TalkSasaContact> asContact() {
            return Optional.ofNullable(contact);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContactBody(
            String phone,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName) {
    }
}
