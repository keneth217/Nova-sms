package com.novastack.sms.mpesa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaDarajaClient {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppProperties appProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public StkPushResult initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String callbackUrl) {
        AppProperties.Mpesa mpesa = appProperties.getMpesa();
        validateConfig(mpesa);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = buildPassword(mpesa, timestamp);

        int amountInt = amount.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        if (amountInt < 1) {
            throw new ApiException("Minimum top-up is 1 KES", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("BusinessShortCode", mpesa.getShortcode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", "CustomerPayBillOnline");
        body.put("Amount", amountInt);
        body.put("PartyA", phoneNumber);
        body.put("PartyB", mpesa.getShortcode());
        body.put("PhoneNumber", phoneNumber);
        body.put("CallBackURL", callbackUrl);
        body.put("AccountReference", truncate(accountReference, 12));
        body.put("TransactionDesc", truncate(mpesa.getTransactionDesc(), 13));

        try {
            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(mpesa.getBaseUrl() + "/mpesa/stkpush/v1/processrequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String responseCode = root.path("ResponseCode").asText();
            if (!"0".equals(responseCode)) {
                String error = root.path("ResponseDescription").asText(root.path("errorMessage").asText("STK push failed"));
                throw new ApiException("M-Pesa STK Push failed: " + error, HttpStatus.BAD_GATEWAY);
            }

            return new StkPushResult(
                    root.path("CheckoutRequestID").asText(),
                    root.path("MerchantRequestID").asText(),
                    root.path("CustomerMessage").asText("Check your phone to complete payment"),
                    responseBody
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("Daraja STK HTTP error: {}", ex.getResponseBodyAsString());
            throw new ApiException("M-Pesa STK Push failed: " + ex.getResponseBodyAsString(), HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Daraja STK error: {}", ex.getMessage(), ex);
            throw new ApiException("M-Pesa STK Push failed: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Queries Safaricom for STK Push transaction status (fallback when callback is delayed).
     */
    public StkQueryResult queryStkStatus(String checkoutRequestId) {
        AppProperties.Mpesa mpesa = appProperties.getMpesa();
        validateConfig(mpesa);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String password = buildPassword(mpesa, timestamp);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("BusinessShortCode", mpesa.getShortcode());
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("CheckoutRequestID", checkoutRequestId);

        try {
            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(mpesa.getBaseUrl() + "/mpesa/stkpushquery/v1/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            return new StkQueryResult(
                    root.path("ResponseCode").asText(null),
                    root.path("ResponseDescription").asText(null),
                    root.path("MerchantRequestID").asText(null),
                    root.path("CheckoutRequestID").asText(checkoutRequestId),
                    root.path("ResultCode").asText(null),
                    root.path("ResultDesc").asText(null),
                    responseBody
            );
        } catch (RestClientResponseException ex) {
            log.error("Daraja STK query HTTP error: {}", ex.getResponseBodyAsString());
            throw new ApiException("M-Pesa STK query failed: " + ex.getResponseBodyAsString(), HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Daraja STK query error: {}", ex.getMessage(), ex);
            throw new ApiException("M-Pesa STK query failed: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private String buildPassword(AppProperties.Mpesa mpesa, String timestamp) {
        return Base64.getEncoder().encodeToString(
                (mpesa.getShortcode() + mpesa.getPasskey() + timestamp).getBytes(StandardCharsets.UTF_8));
    }

    private String getAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAtMs() > System.currentTimeMillis() + 30_000) {
            return current.token();
        }

        AppProperties.Mpesa mpesa = appProperties.getMpesa();
        String basic = Base64.getEncoder().encodeToString(
                (mpesa.getConsumerKey() + ":" + mpesa.getConsumerSecret()).getBytes(StandardCharsets.UTF_8));

        try {
            String responseBody = restClientBuilder.build()
                    .get()
                    .uri(mpesa.getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                    .header("Authorization", "Basic " + basic)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String token = root.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new ApiException("Failed to obtain M-Pesa access token", HttpStatus.BAD_GATEWAY);
            }
            long expiresIn = root.path("expires_in").asLong(3599);
            cachedToken.set(new CachedToken(token, System.currentTimeMillis() + (expiresIn * 1000)));
            return token;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Daraja OAuth error: {}", ex.getMessage(), ex);
            throw new ApiException("Failed to authenticate with M-Pesa Daraja: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private void validateConfig(AppProperties.Mpesa mpesa) {
        if (isBlank(mpesa.getConsumerKey()) || isBlank(mpesa.getConsumerSecret())
                || isBlank(mpesa.getPasskey()) || isBlank(mpesa.getShortcode())) {
            throw new ApiException(
                    "M-Pesa Daraja is not configured. Set consumer key/secret, passkey, and paybill shortcode.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record StkPushResult(
            String checkoutRequestId,
            String merchantRequestId,
            String customerMessage,
            String rawResponse
    ) {
    }

    public record StkQueryResult(
            String responseCode,
            String responseDescription,
            String merchantRequestId,
            String checkoutRequestId,
            String resultCode,
            String resultDesc,
            String rawResponse
    ) {
        public boolean isPaymentSuccessful() {
            return "0".equals(resultCode);
        }

        public boolean isTerminalFailure() {
            if (resultCode == null || resultCode.isBlank() || "0".equals(resultCode)) {
                return false;
            }
            String desc = resultDesc != null ? resultDesc.toLowerCase() : "";
            return !desc.contains("being processed");
        }
    }

    private record CachedToken(String token, long expiresAtMs) {
    }
}