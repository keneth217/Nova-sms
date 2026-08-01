package com.novastack.sms.databundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafaricomAuthService {

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public String getAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return current.token();
        }
        return refreshAccessToken();
    }

    public void invalidateToken() {
        cachedToken.set(null);
    }

    private String refreshAccessToken() {
        AppProperties.DataBundles cfg = appProperties.getDataBundles();
        // Same Daraja app credentials as M-Pesa are supported (shared Consumer Key/Secret).
        String key = firstNonBlank(cfg.getConsumerKey(), appProperties.getMpesa().getConsumerKey());
        String secret = firstNonBlank(cfg.getConsumerSecret(), appProperties.getMpesa().getConsumerSecret());
        boolean usingMpesaFallback = isBlank(cfg.getConsumerKey()) || isBlank(cfg.getConsumerSecret());
        if (isBlank(key) || isBlank(secret)) {
            throw new ApiException(
                    "Safaricom credentials missing. Set DATA_BUNDLES_CONSUMER_KEY/SECRET "
                            + "or MPESA_CONSUMER_KEY/SECRET (they can be the same Daraja app).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String basic = Base64.getEncoder().encodeToString((key + ":" + secret).getBytes(StandardCharsets.UTF_8));
        String url = trimSlash(cfg.getBaseUrl()) + cfg.getOauthPath() + "?grant_type=client_credentials";

        try {
            log.info(
                    "Requesting Safaricom Dynamic Offers OAuth token baseUrl={} credentialSource={}",
                    cfg.getBaseUrl(),
                    usingMpesaFallback ? "mpesa-fallback" : "data-bundles");
            String body = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(20));

            JsonNode root = objectMapper.readTree(body);
            String token = root.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new ApiException("Safaricom OAuth did not return access_token", HttpStatus.BAD_GATEWAY);
            }
            long expiresIn = root.path("expires_in").asLong(3599);
            CachedToken next = new CachedToken(token, Instant.now().plusSeconds(Math.max(60, expiresIn)));
            cachedToken.set(next);
            log.info("Safaricom Dynamic Offers OAuth token acquired, expiresIn={}s", expiresIn);
            return token;
        } catch (ApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Safaricom OAuth HTTP {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw SafaricomApiErrorMapper.fromHttp("oauth", ex);
        } catch (Exception ex) {
            log.error("Safaricom OAuth error: {}", ex.getMessage(), ex);
            throw SafaricomApiErrorMapper.fromThrowable("oauth", ex);
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
