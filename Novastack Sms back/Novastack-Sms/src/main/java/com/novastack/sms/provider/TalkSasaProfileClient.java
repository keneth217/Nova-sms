package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.TalkSasaAccountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class TalkSasaProfileClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public TalkSasaProfileClient(
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

    public TalkSasaAccountResponse getAccount() {
        if (!isConfigured()) {
            return TalkSasaAccountResponse.builder()
                    .configured(false)
                    .reachable(false)
                    .errorMessage(ProviderErrorMessages.NOT_CONFIGURED)
                    .build();
        }

        FetchResult profileResult = getJson("/me");
        FetchResult balanceResult = getJson("/balance");
        TalkSasaAccountResponse.Profile profile = parseProfile(profileResult.data());
        TalkSasaAccountResponse.Balance balance = parseBalance(balanceResult.data());
        boolean reachable = profileResult.ok() || balanceResult.ok();
        String error = null;
        if (!reachable) {
            error = firstNonBlank(profileResult.error(), balanceResult.error());
            if (error == null) {
                error = ProviderErrorMessages.UNAVAILABLE;
            }
        }

        return TalkSasaAccountResponse.builder()
                .configured(true)
                .reachable(reachable)
                .errorMessage(error)
                .profile(profile)
                .balance(balance)
                .build();
    }

    private FetchResult getJson(String path) {
        try {
            String body = webClient.get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(readTimeout());
            JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
            if (isErrorStatus(root)) {
                log.warn("TalkSasa {} returned error", path);
                return FetchResult.fail(ProviderErrorMessages.VALIDATION);
            }
            return FetchResult.ok(extractData(root));
        } catch (WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("TalkSasa profile HTTP error path={} status={} body={}",
                    path, status, abbreviate(redact(ex.getResponseBodyAsString())));
            return FetchResult.fail(ProviderErrorMessages.forHttpStatus(status));
        } catch (WebClientRequestException ex) {
            log.error("TalkSasa profile connection failure path={} message={}", path, safeMessage(ex));
            return FetchResult.fail(ProviderErrorMessages.UNAVAILABLE);
        } catch (Exception ex) {
            log.error("TalkSasa profile {} failed: {}", path, safeMessage(ex));
            return FetchResult.fail(ProviderErrorMessages.UNAVAILABLE);
        }
    }

    private TalkSasaAccountResponse.Profile parseProfile(JsonNode data) {
        if (data == null || data.isNull() || data.isMissingNode()) {
            return null;
        }
        String first = firstText(data, "first_name", "firstName");
        String last = firstText(data, "last_name", "lastName");
        String combined = joinName(first, last);
        String name = firstText(data, "name", "full_name", "company");
        if (name == null) {
            name = combined;
        }
        return TalkSasaAccountResponse.Profile.builder()
                .name(blankToNull(name))
                .email(firstText(data, "email"))
                .phone(firstText(data, "phone", "mobile", "phone_number"))
                .country(firstText(data, "country"))
                .timezone(firstText(data, "timezone", "time_zone"))
                .status(firstText(data, "status"))
                .build();
    }

    private TalkSasaAccountResponse.Balance parseBalance(JsonNode data) {
        if (data == null || data.isNull() || data.isMissingNode()) {
            return null;
        }
        BigDecimal remaining = firstDecimal(
                data,
                "remaining_units",
                "remaining_balance",
                "remaining",
                "credit",
                "balance",
                "sms_unit",
                "sms_units");
        BigDecimal total = firstDecimal(data, "total_units", "total_balance", "total");
        BigDecimal used = firstDecimal(data, "used_units", "used_balance", "used");
        if (remaining == null && total == null && used == null) {
            return null;
        }
        return TalkSasaAccountResponse.Balance.builder()
                .remainingUnits(remaining)
                .totalUnits(total)
                .usedUnits(used)
                .unitType(Optional.ofNullable(firstText(data, "unit_type", "currency", "unit")).orElse("SMS"))
                .expiredOn(firstText(data, "expired_on", "expires_on", "last_updated"))
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

    private static BigDecimal firstDecimal(JsonNode node, String... fields) {
        String text = firstText(node, fields);
        if (text == null) {
            return null;
        }
        String cleaned = text.replace(",", "").replaceAll("[^0-9.+-]", "");
        if (cleaned.isBlank() || "-".equals(cleaned) || "+".equals(cleaned) || ".".equals(cleaned)) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String joinName(String first, String last) {
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return (first + " " + last).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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

    private record FetchResult(boolean ok, JsonNode data, String error) {
        static FetchResult ok(JsonNode data) {
            return new FetchResult(true, data, null);
        }

        static FetchResult fail(String error) {
            return new FetchResult(false, null, error);
        }
    }
}
