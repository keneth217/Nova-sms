package com.novastack.sms.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TalkSasaContactGroupClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public TalkSasaContactGroupClient(
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

    public Optional<TalkSasaGroup> create(String name) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return mutate("POST", "/contacts", new GroupNameBody(name)).asGroup();
    }

    public Optional<TalkSasaGroup> get(String uid) {
        if (!isEnabled() || uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        return mutate("POST", "/contacts/" + uid.trim() + "/show", null).asGroup();
    }

    public Optional<TalkSasaGroup> update(String uid, String name) {
        if (!isEnabled() || uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        return mutate("PATCH", "/contacts/" + uid.trim(), new GroupNameBody(name)).asGroup();
    }

    public boolean delete(String uid) {
        if (!isEnabled() || uid == null || uid.isBlank()) {
            return false;
        }
        return mutate("DELETE", "/contacts/" + uid.trim(), null).success();
    }

    public List<TalkSasaGroup> list() {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            String body = exchange("GET", "/contacts/", null);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa list contact groups returned error");
                return List.of();
            }
            return parseGroupList(root);
        } catch (Exception ex) {
            log.warn("TalkSasa list contact groups failed: {}", safeMessage(ex));
            return List.of();
        }
    }

    private CallResult mutate(String method, String path, Object payload) {
        try {
            String body = exchange(method, path, payload);
            JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa contact group {} {} returned error", method, path);
                return CallResult.failure();
            }
            return new CallResult(true, parseGroup(root).orElse(null));
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("TalkSasa contact group HTTP error method={} path={} status={} body={}",
                    method, path, status, abbreviate(redact(ex.getResponseBodyAsString())));
            return CallResult.failure();
        } catch (WebClientRequestException ex) {
            log.error("TalkSasa contact group connection failure method={} path={} message={}",
                    method, path, safeMessage(ex));
            return CallResult.failure();
        } catch (Exception ex) {
            log.error("TalkSasa contact group {} {} failed: {}", method, path, safeMessage(ex));
            return CallResult.failure();
        }
    }

    private String exchange(String method, String path, Object payload) {
        String token = apiToken();
        WebClient.RequestBodySpec spec = webClient.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri(path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        WebClient.RequestHeadersSpec<?> request = payload == null
                ? spec
                : spec.contentType(MediaType.APPLICATION_JSON).bodyValue(payload);
        return request.retrieve().bodyToMono(String.class).block(readTimeout());
    }

    private Optional<TalkSasaGroup> parseGroup(JsonNode root) {
        JsonNode data = extractData(root);
        if (data == null || data.isNull() || data.isMissingNode() || "null".equalsIgnoreCase(data.asText(""))) {
            return Optional.empty();
        }
        if (data.isArray() && data.size() > 0) {
            data = data.get(0);
        }
        String uid = firstText(data, "uid", "id", "group_id", "group_uid");
        String name = firstText(data, "name", "group_name");
        if (uid == null || uid.isBlank()) {
            uid = firstText(root, "uid", "id");
        }
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TalkSasaGroup(uid, name, data));
    }

    private List<TalkSasaGroup> parseGroupList(JsonNode root) {
        List<TalkSasaGroup> groups = new ArrayList<>();
        JsonNode data = extractData(root);
        if (data == null) {
            return groups;
        }
        if (data.isArray()) {
            data.forEach(item -> parseGroupItem(item).ifPresent(groups::add));
            return groups;
        }
        JsonNode nested = data.path("data");
        if (nested.isArray()) {
            nested.forEach(item -> parseGroupItem(item).ifPresent(groups::add));
            return groups;
        }
        parseGroup(root).ifPresent(groups::add);
        return groups;
    }

    private Optional<TalkSasaGroup> parseGroupItem(JsonNode item) {
        String uid = firstText(item, "uid", "id", "group_id", "group_uid");
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TalkSasaGroup(uid, firstText(item, "name", "group_name"), item));
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

    public record TalkSasaGroup(String uid, String name, JsonNode raw) {
    }

    private record CallResult(boolean success, TalkSasaGroup group) {
        static CallResult failure() {
            return new CallResult(false, null);
        }

        Optional<TalkSasaGroup> asGroup() {
            return Optional.ofNullable(group);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GroupNameBody(String name) {
    }
}
