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

            return parseStkQueryResponse(checkoutRequestId, responseBody);
        } catch (RestClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.warn("Daraja STK query HTTP error: {}", errorBody);
            StkQueryResult parsed = parseStkQueryResponse(checkoutRequestId, errorBody);
            if (parsed.hasProcessingDescriptor()) {
                return parsed;
            }
            throw new ApiException("M-Pesa STK query failed: " + errorBody, HttpStatus.BAD_GATEWAY);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Daraja STK query error: {}", ex.getMessage(), ex);
            throw new ApiException("M-Pesa STK query failed: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    StkQueryResult parseStkQueryResponse(String checkoutRequestId, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new StkQueryResult(null, null, null, checkoutRequestId, null, null, responseBody);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String resultCode = firstText(root, "ResultCode", "errorCode");
            String resultDesc = firstText(root, "ResultDesc", "errorMessage", "ResponseDescription");
            return new StkQueryResult(
                    firstText(root, "ResponseCode", "errorCode"),
                    firstText(root, "ResponseDescription", "errorMessage"),
                    textOrNull(root, "MerchantRequestID"),
                    firstText(root, "CheckoutRequestID") != null
                            ? firstText(root, "CheckoutRequestID")
                            : checkoutRequestId,
                    resultCode,
                    resultDesc,
                    responseBody
            );
        } catch (Exception ex) {
            return new StkQueryResult(null, null, null, checkoutRequestId, null, responseBody, responseBody);
        }
    }

    /**
     * Asks Safaricom for the status of a Paybill/STK receipt when the C2B callback never arrived.
     * Asynchronous: Daraja posts the result to {@code resultUrl}.
     */
    public TransactionStatusSubmitResult queryTransactionStatus(String transactionId, String resultUrl, String timeoutUrl) {
        AppProperties.Mpesa mpesa = appProperties.getMpesa();
        validateConfig(mpesa);
        if (!MpesaSecurityCredential.configured(mpesa)) {
            throw new ApiException(
                    "M-Pesa Transaction Status is not configured. Set MPESA_INITIATOR_NAME and MPESA_SECURITY_CREDENTIAL.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (isBlank(transactionId)) {
            throw new ApiException("M-Pesa receipt is required", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("Initiator", mpesa.getInitiatorName().trim());
        body.put("SecurityCredential", MpesaSecurityCredential.resolve(mpesa));
        body.put("CommandID", "TransactionStatusQuery");
        body.put("TransactionID", transactionId.trim());
        body.put("OriginatorConversationID", "");
        body.put("PartyA", mpesa.getShortcode());
        body.put("IdentifierType", "4");
        body.put("ResultURL", resultUrl);
        body.put("QueueTimeoutURL", timeoutUrl);
        body.put("Remarks", "OK");
        body.put("Occasion", "OK");

        try {
            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(mpesa.getBaseUrl() + "/mpesa/transactionstatus/v1/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            String responseCode = firstText(root, "ResponseCode", "errorCode");
            String description = firstText(root, "ResponseDescription", "errorMessage");
            boolean accepted = "0".equals(responseCode)
                    || (description != null && description.toLowerCase().contains("accept"));
            if (!accepted) {
                throw new ApiException(
                        "M-Pesa Transaction Status failed: " + (description == null ? responseBody : description),
                        HttpStatus.BAD_GATEWAY);
            }
            return new TransactionStatusSubmitResult(
                    firstText(root, "OriginatorConversationID"),
                    firstText(root, "ConversationID"),
                    responseCode,
                    description,
                    responseBody);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("Daraja Transaction Status HTTP error: {}", ex.getResponseBodyAsString());
            throw new ApiException("M-Pesa Transaction Status failed: " + ex.getResponseBodyAsString(),
                    HttpStatus.BAD_GATEWAY);
        } catch (Exception ex) {
            log.error("Daraja Transaction Status error: {}", ex.getMessage(), ex);
            throw new ApiException("M-Pesa Transaction Status failed: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public boolean isTransactionStatusConfigured() {
        return MpesaSecurityCredential.configured(appProperties.getMpesa());
    }

    /**
     * Register C2B v2 validation/confirmation URLs for the Paybill shortcode.
     * Safaricom rejects callback URLs that contain the word "mpesa".
     */
    public C2bRegisterResult registerC2bUrls(String confirmationUrl, String validationUrl) {
        AppProperties.Mpesa mpesa = appProperties.getMpesa();
        validateConfig(mpesa);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("ShortCode", mpesa.getShortcode());
        body.put("ResponseType", "Completed");
        body.put("ConfirmationURL", confirmationUrl);
        body.put("ValidationURL", validationUrl);

        try {
            return postRegisterUrl(body, false);
        } catch (RestClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            if (C2bDarajaErrorMapper.invalidAccessToken(errorBody)) {
                cachedToken.set(null);
                try {
                    return postRegisterUrl(body, true);
                } catch (RestClientResponseException retryEx) {
                    return registerFailure(retryEx.getStatusCode().value(), retryEx.getResponseBodyAsString());
                } catch (Exception retryEx) {
                    throw new ApiException("C2B URL registration failed: " + retryEx.getMessage(),
                            HttpStatus.BAD_GATEWAY);
                }
            }
            return registerFailure(ex.getStatusCode().value(), errorBody);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException("C2B URL registration failed: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private C2bRegisterResult postRegisterUrl(Map<String, Object> body, boolean retried) {
        String responseBody = restClientBuilder.build()
                .post()
                .uri(appProperties.getMpesa().getBaseUrl() + "/mpesa/c2b/v2/registerurl")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getAccessToken())
                .body(body)
                .retrieve()
                .body(String.class);
        log.info("C2B v2 registerurl response retried={}: {}", retried, responseBody);
        String code = extractDarajaCode(responseBody);
        String desc = extractDarajaDescription(responseBody);
        boolean ok = "0".equals(code) || (desc != null && desc.equalsIgnoreCase("Success"));
        return new C2bRegisterResult(ok, false, code, desc != null ? desc : "Success", responseBody);
    }

    private C2bRegisterResult registerFailure(int httpStatus, String errorBody) {
        if (C2bDarajaErrorMapper.alreadyRegistered(errorBody)) {
            log.info("C2B v2 URLs already registered: {}", errorBody);
            return new C2bRegisterResult(true, true, "500.003.1001",
                    C2bDarajaErrorMapper.message(httpStatus, errorBody), errorBody);
        }
        String mapped = C2bDarajaErrorMapper.message(httpStatus, errorBody);
        log.error("C2B v2 registerurl HTTP {}: {}", httpStatus, errorBody);
        throw new ApiException(mapped, HttpStatus.BAD_GATEWAY);
    }

    private String extractDarajaCode(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = firstText(root, "ResponseCode", "errorCode");
            return code;
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractDarajaDescription(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return firstText(root, "ResponseDescription", "errorMessage");
        } catch (Exception ex) {
            return body;
        }
    }

    private String firstText(JsonNode root, String... fields) {
        for (String field : fields) {
            String value = textOrNull(root, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String textOrNull(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
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

    public record TransactionStatusSubmitResult(
            String originatorConversationId,
            String conversationId,
            String responseCode,
            String responseDescription,
            String rawResponse
    ) {
    }

    public record C2bRegisterResult(
            boolean success,
            boolean alreadyRegistered,
            String errorCode,
            String message,
            String rawResponse
    ) {
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

        /**
         * Safaricom has not finished the STK request yet. This is not a payment failure.
         * Typical text: "The transaction is still under processing".
         */
        public boolean isStillProcessing() {
            if (isPaymentSuccessful()) {
                return false;
            }
            if (hasProcessingDescriptor()) {
                return true;
            }
            return resultCode == null || resultCode.isBlank();
        }

        /** True only when Safaricom explicitly said the STK request is still in flight. */
        public boolean hasProcessingDescriptor() {
            String blob = ((responseCode == null ? "" : responseCode) + " "
                    + (responseDescription == null ? "" : responseDescription) + " "
                    + (resultCode == null ? "" : resultCode) + " "
                    + (resultDesc == null ? "" : resultDesc)).toLowerCase();
            if (blob.contains("under processing")
                    || blob.contains("being processed")
                    || blob.contains("still processing")
                    || blob.contains("request processing")
                    || blob.contains("in progress")) {
                return true;
            }
            return "4999".equals(resultCode)
                    || "500.001.1001".equals(resultCode)
                    || "500.001.1001".equals(responseCode);
        }

        public boolean isTerminalFailure() {
            return !isPaymentSuccessful() && !isStillProcessing();
        }
    }

    private record CachedToken(String token, long expiresAtMs) {
    }
}