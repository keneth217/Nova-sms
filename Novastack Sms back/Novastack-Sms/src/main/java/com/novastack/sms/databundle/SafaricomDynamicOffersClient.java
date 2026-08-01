package com.novastack.sms.databundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.DataBundleOfferResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Safaricom Dynamic Offers client (Daraja).
 * <p>
 * Production:
 * GET  /v1/dynamic-offers/fetch?msisdn=
 * POST /v1/dynamic-offers/facebook-bundle/purchase
 * GET  /v2/bundles/get/status?id=&serviceAccountId=0
 * Base: https://api.safaricom.co.ke
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafaricomDynamicOffersClient {

    private final AppProperties appProperties;
    private final SafaricomAuthService safaricomAuthService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Fetch validates eligibility and returns offers in one call (per Safaricom docs).
     */
    public void validateSubscriber(String phoneNumber) {
        List<DataBundleOfferResponse> offers = fetchOffers(phoneNumber);
        if (offers.isEmpty()) {
            throw new ApiException(
                    "This number is not eligible for data bundle offers right now.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    public List<DataBundleOfferResponse> fetchOffers(String phoneNumber) {
        AppProperties.DataBundles cfg = appProperties.getDataBundles();
        String url = UriComponentsBuilder
                .fromUriString(trimSlash(cfg.getBaseUrl()) + cfg.getOffersPath())
                .queryParam("msisdn", phoneNumber)
                .build(true)
                .toUriString();

        try {
            log.info("Safaricom fetch offers url={}", url);
            String responseBody = authorizedGet(url);
            log.info("Safaricom fetch offers response={}", abbreviate(responseBody));
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            SafaricomApiErrorMapper.assertOffersSuccess(root);
            List<DataBundleOfferResponse> offers = parseOffers(root);
            if (offers.isEmpty()) {
                throw new ApiException(
                        "No data bundle offers are available for this number right now.",
                        HttpStatus.NOT_FOUND);
            }
            return offers;
        } catch (ApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Safaricom fetch offers HTTP {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw SafaricomApiErrorMapper.fromHttp("fetch offers", ex);
        } catch (Exception ex) {
            log.error("Safaricom fetch offers error: {}", ex.getMessage(), ex);
            throw SafaricomApiErrorMapper.fromThrowable("fetch offers", ex);
        }
    }

    public PurchaseResult purchase(PurchaseCommand command) {
        // Field order + types match Safaricom purchase sample (string values).
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("offeringId", command.offeringId());
        body.put("accountId", command.accountId());
        body.put("price", stripDecimals(command.price()));
        body.put("resourceAmount", command.resourceAmount() == null ? "0" : command.resourceAmount());
        body.put("validity", command.validity() == null ? "1" : command.validity());
        // Body uses international 254… (same as fetch). National form caused fulfilment mismatches.
        body.put("msisdn", toInternationalMsisdn(command.msisdn()));
        body.put("transactionId", command.transactionId());
        body.put("paymentMode", normalizePaymentMode(command.paymentMode()));

        AppProperties.DataBundles cfg = appProperties.getDataBundles();
        String url = trimSlash(cfg.getBaseUrl()) + cfg.getPurchasePath();

        try {
            String jsonBody;
            try {
                jsonBody = objectMapper.writeValueAsString(body);
            } catch (Exception ex) {
                jsonBody = String.valueOf(body);
            }
            log.info("Safaricom purchase url={} json={} txId={}", url, jsonBody, command.transactionId());
            PurchaseHttpResponse http = authorizedPurchasePost(url, body, command);
            log.info(
                    "Safaricom purchase HTTP {} contentType={} body={}",
                    http.status(),
                    http.contentType(),
                    abbreviate(http.body(), 2000));
            if (http.status() >= 400) {
                throw SafaricomApiErrorMapper.fromBody(
                        "purchase",
                        http.status(),
                        http.body() == null || http.body().isBlank() ? "{}" : http.body(),
                        String.valueOf(http.status()));
            }
            JsonNode root = parseJsonFlexible(http.body());
            SafaricomApiErrorMapper.assertPurchaseSuccess(root);

            PurchaseResult result = parsePurchaseResult(root, command.transactionId());
            if (result.responseCode() == null || result.responseCode().isBlank()) {
                log.warn(
                        "Safaricom purchase returned empty body (HTTP {}). Polling status for txId={}",
                        http.status(),
                        command.transactionId());
                PurchaseResult fromStatus = resolveEmptyPurchaseViaStatus(command.transactionId());
                if (fromStatus != null) {
                    return fromStatus;
                }
                log.error(
                        "Safaricom purchase returned no responseCode and status poll found nothing. HTTP {} contentType={} body={}",
                        http.status(),
                        http.contentType(),
                        http.body());
                throw new ApiException(
                        "Safaricom returned an empty purchase response (HTTP "
                                + http.status()
                                + "). Please retry in a moment.",
                        HttpStatus.BAD_GATEWAY);
            }
            return result;
        } catch (ApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Safaricom purchase HTTP {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw SafaricomApiErrorMapper.fromHttp("purchase", ex);
        } catch (Exception ex) {
            log.error("Safaricom purchase error: {}", ex.getMessage(), ex);
            throw SafaricomApiErrorMapper.fromThrowable("purchase", ex);
        }
    }

    /**
     * Airtime sometimes returns HTTP 200 with {@code {}}. Check status before treating as failure.
     */
    private PurchaseResult resolveEmptyPurchaseViaStatus(String transactionId) {
        try {
            StatusResult status = queryStatus(transactionId);
            log.info(
                    "Empty-purchase status poll txId={} status={} code={} desc={}",
                    transactionId,
                    status.status(),
                    status.responseCode(),
                    status.responseDescription());
            if ("SUCCESS".equals(status.status())
                    || SafaricomApiErrorMapper.isSuccess(status.responseCode())) {
                return new PurchaseResult(
                        firstNonBlank(status.responseCode(), "200"),
                        firstNonBlank(status.responseDescription(), "Bundle purchase was successful"),
                        null,
                        transactionId);
            }
            if ("PENDING".equals(status.status()) || isPendingStatus(status.responseCode())) {
                return new PurchaseResult(
                        firstNonBlank(status.responseCode(), "PENDING"),
                        firstNonBlank(status.responseDescription(), "Purchase accepted; confirmation pending"),
                        null,
                        transactionId);
            }
            if ("FAILED".equals(status.status()) || "CANCELLED".equals(status.status())) {
                throw new ApiException(
                        firstNonBlank(status.responseDescription(), "Bundle purchase failed"),
                        HttpStatus.BAD_REQUEST);
            }
            return null;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Empty-purchase status poll failed for txId={}: {}", transactionId, ex.getMessage());
            return null;
        }
    }

    private PurchaseResult parsePurchaseResult(JsonNode root, String transactionId) {
        JsonNode header = root.path("header");
        JsonNode primary = header.isMissingNode() || header.isNull() ? root : header;

        String code = firstNonBlank(
                text(primary, "responseCode", "ResponseCode", "status", "Status", "responseStatus"),
                text(root, "responseCode", "ResponseCode", "status", "Status", "responseStatus"),
                text(root, "errorCode", "ErrorCode", "resultCode", "ResultCode"));
        String description = firstNonBlank(
                text(primary, "customerMessage", "CustomerMessage", "customerMsg", "CustomerMsg"),
                text(primary, "responseMessage", "ResponseMessage", "responseMsg", "ResponseMsg", "desc"),
                text(root, "customerMessage", "CustomerMessage", "CustomerMsg", "customerMsg"),
                text(root, "responseMessage", "ResponseMessage", "ResponseMsg", "responseDesc", "desc",
                        "errorMessage", "ErrorMessage", "message"));
        String requestRef = firstNonBlank(
                text(primary, "requestRefId", "RequestRefId", "requestRefID", "requestId"),
                text(root, "requestRefId", "RequestRefId", "requestRefID", "requestId", "id"));

        return new PurchaseResult(code, description, requestRef, transactionId);
    }

    private JsonNode parseJsonFlexible(String responseBody) throws Exception {
        String raw = responseBody == null || responseBody.isBlank() ? "{}" : responseBody.trim();
        JsonNode root = objectMapper.readTree(raw);
        // Some gateways double-encode JSON as a string.
        if (root.isTextual()) {
            String inner = root.asText();
            if (inner != null && !inner.isBlank()) {
                root = objectMapper.readTree(inner);
            }
        }
        return root;
    }

    public StatusResult queryStatus(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new ApiException("Missing Safaricom transaction id for status check", HttpStatus.BAD_REQUEST);
        }

        AppProperties.DataBundles cfg = appProperties.getDataBundles();
        String url = UriComponentsBuilder
                .fromUriString(trimSlash(cfg.getBaseUrl()) + cfg.getStatusPath())
                .queryParam("id", transactionId)
                .queryParam("serviceAccountId", cfg.getServiceAccountId())
                .build(true)
                .toUriString();

        try {
            log.info("Safaricom status url={}", url);
            String responseBody = authorizedGet(url);
            log.info("Safaricom status response={}", abbreviate(responseBody));
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            String statusCode = text(root, "responseStatus", "ResponseStatus", "status", "Status");
            String desc = text(root, "responseDesc", "ResponseDesc", "responseDescription", "desc");
            if (statusCode != null && !SafaricomApiErrorMapper.isSuccess(statusCode)
                    && !isPendingStatus(statusCode)) {
                throw SafaricomApiErrorMapper.fromBody("status", parseIntSafe(statusCode, 400),
                        desc == null ? root.toString() : desc, statusCode);
            }
            return new StatusResult(
                    mapStatusLabel(statusCode, desc),
                    statusCode,
                    desc);
        } catch (ApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Safaricom status HTTP {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw SafaricomApiErrorMapper.fromHttp("status", ex);
        } catch (Exception ex) {
            log.error("Safaricom status error: {}", ex.getMessage(), ex);
            throw SafaricomApiErrorMapper.fromThrowable("status", ex);
        }
    }

    private String authorizedGet(String url) {
        return authorizedGet(url, true);
    }

    private String authorizedGet(String url, boolean retryOnUnauthorized) {
        try {
            // Status endpoint expects Content-Type: application/json even on GET (Daraja quirk).
            return webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + safaricomAuthService.getAccessToken())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException ex) {
            if (retryOnUnauthorized && ex.getStatusCode().value() == 401) {
                log.warn("Safaricom returned 401 — refreshing OAuth token and retrying once");
                safaricomAuthService.invalidateToken();
                return authorizedGet(url, false);
            }
            throw ex;
        }
    }

    /**
     * Dynamic Offers purchase requires partner headers (missing → "Request Header Invalid"):
     * x-source-system, x-correlation-conversationid, x-msisdn, x-key-type.
     */
    private PurchaseHttpResponse authorizedPurchasePost(
            String url, Map<String, Object> body, PurchaseCommand command) {
        return authorizedPurchasePost(url, body, command, true);
    }

    private PurchaseHttpResponse authorizedPurchasePost(
            String url,
            Map<String, Object> body,
            PurchaseCommand command,
            boolean retryOnUnauthorized) {
        AppProperties.DataBundles cfg = appProperties.getDataBundles();
        String sourceSystem = firstNonBlank(cfg.getSourceSystem(), "fb");
        String correlationId = command.transactionId();
        String headerMsisdn = toInternationalMsisdn(command.msisdn());
        try {
            PurchaseHttpResponse http = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + safaricomAuthService.getAccessToken())
                    .header("x-source-system", sourceSystem)
                    .header("x-correlation-conversationid", correlationId)
                    .header("x-msisdn", headerMsisdn)
                    .header("x-key-type", "0")
                    .bodyValue(body)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(bodyText -> new PurchaseHttpResponse(
                                    response.statusCode().value(),
                                    response.headers().contentType()
                                            .map(MediaType::toString)
                                            .orElse(""),
                                    bodyText)))
                    .block(Duration.ofSeconds(30));
            if (http == null) {
                throw new ApiException("Safaricom purchase returned no HTTP response", HttpStatus.BAD_GATEWAY);
            }
            if (retryOnUnauthorized && http.status() == 401) {
                log.warn("Safaricom purchase returned 401 — refreshing OAuth token and retrying once");
                safaricomAuthService.invalidateToken();
                return authorizedPurchasePost(url, body, command, false);
            }
            return http;
        } catch (WebClientResponseException ex) {
            if (retryOnUnauthorized && ex.getStatusCode().value() == 401) {
                log.warn("Safaricom purchase returned 401 — refreshing OAuth token and retrying once");
                safaricomAuthService.invalidateToken();
                return authorizedPurchasePost(url, body, command, false);
            }
            throw ex;
        }
    }

    private record PurchaseHttpResponse(int status, String contentType, String body) {
    }

    /** Visible for unit tests — parses Safaricom fetch-offers JSON. */
    List<DataBundleOfferResponse> parseOffersResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
        SafaricomApiErrorMapper.assertOffersSuccess(root);
        return parseOffers(root);
    }

    private List<DataBundleOfferResponse> parseOffers(JsonNode root) {
        List<DataBundleOfferResponse> offers = new ArrayList<>();
        JsonNode characteristics = root.path("lineItem").path("characteristicsValue");
        if (characteristics.isArray()) {
            for (JsonNode item : characteristics) {
                addOffer(offers, item, null);
                JsonNode children = item.path("childOffers");
                if (children.isArray()) {
                    String parentId = text(item, "offeringId", "uniqueOfferingId");
                    for (JsonNode child : children) {
                        addOffer(offers, child, parentId);
                    }
                }
            }
            return offers;
        }

        JsonNode array = firstArray(root, "offers", "Offers", "OfferList", "data", "Data");
        if (array == null && root.isArray()) {
            array = root;
        }
        if (array != null) {
            for (JsonNode item : array) {
                addOffer(offers, item, null);
            }
        }
        return offers;
    }

    private void addOffer(List<DataBundleOfferResponse> offers, JsonNode item, String parentOfferId) {
        // Docs: purchase offeringId must be the fetch "offeringId" (unique product number).
        // Do NOT fall back to uniqueOfferingId / resourceAccId — that caused mismatched purchases.
        String offeringId = text(item, "offeringId");
        String uniqueOfferingId = text(item, "uniqueOfferingId");
        if (offeringId == null || offeringId.isBlank()) {
            log.warn(
                    "Skipping Safaricom offer without offeringId name={} uniqueOfferingId={} resourceAccId={}",
                    text(item, "offerName", "offerUssdName"),
                    uniqueOfferingId,
                    text(item, "resourceAccId"));
            return;
        }
        String name = text(item, "offerName", "offerUssdName", "OfferName", "name");
        BigDecimal amount = decimal(item, "offerPrice", "price", "Price", "amount", "Amount");
        String validityRaw = text(item, "offerValidity", "validity", "Validity");
        String validity = formatValidity(validityRaw);
        String resourceAmount = text(item, "resourceValue", "resourceAmount");
        String accountId = text(item, "resourceAccId", "accountId", "AccountId");
        String source = text(item, "offerSource", "category", "Category");
        String description = buildDescription(name, resourceAmount, validity, item.path("subscribed").asInt(-1));
        String category = categorize(name, validity, source);

        log.debug(
                "Parsed Safaricom offer offeringId={} uniqueOfferingId={} accountId={} price={} resource={} source={}",
                offeringId, uniqueOfferingId, accountId, amount, resourceAmount, source);

        // INFO so production logs show the real offeringId (fetch logs are truncated).
        log.info(
                "Safaricom offer parsed name='{}' offeringId={} uniqueOfferingId={} accountId={} price={} resourceAmount={}",
                name, offeringId, uniqueOfferingId, accountId, amount, resourceAmount);

        offers.add(DataBundleOfferResponse.builder()
                .offerId(offeringId)
                .uniqueOfferingId(uniqueOfferingId)
                .offerName(name == null ? offeringId : name)
                .category(category)
                .amount(amount == null ? BigDecimal.ZERO : amount)
                .validity(validity)
                .description(description)
                .accountId(accountId)
                .resourceAmount(resourceAmount)
                .offerSource(source)
                .parentOfferId(parentOfferId)
                .build());
    }

    static String categorize(String name, String validity, String rawCategory) {
        String hay = ((rawCategory == null ? "" : rawCategory) + " " + (name == null ? "" : name)
                + " " + (validity == null ? "" : validity)).toLowerCase(Locale.ROOT);
        if (hay.contains("promo") || hay.contains("flash") || hay.contains("hot") || hay.contains("cvm")) {
            return "PROMOTIONAL";
        }
        if (hay.contains("month") || hay.contains("30 day") || hay.contains("28 day")) {
            return "MONTHLY";
        }
        if (hay.contains("week") || hay.contains("7 day")) {
            return "WEEKLY";
        }
        if (hay.contains("day") || hay.contains("daily") || hay.contains("hour") || hay.contains("24")) {
            return "DAILY";
        }
        return "OTHER";
    }

    private static String buildDescription(String name, String resourceMb, String validity, int subscribed) {
        StringBuilder sb = new StringBuilder();
        if (resourceMb != null && !resourceMb.isBlank() && !"0".equals(resourceMb)) {
            sb.append(resourceMb).append(" MB");
        }
        if (validity != null && !validity.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(validity);
        }
        if (subscribed == 1) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append("Already subscribed");
        }
        if (sb.isEmpty() && name != null) {
            return name;
        }
        return sb.isEmpty() ? "Safaricom mobile data bundle" : sb.toString();
    }

    private static String formatValidity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.matches("\\d+")) {
            int days = Integer.parseInt(raw);
            if (days <= 0) {
                return raw;
            }
            if (days == 1) {
                return "1 Day";
            }
            return days + " Days";
        }
        return raw;
    }

    /**
     * Purchase body uses international 254… (aligned with fetch + parameter samples).
     */
    public static String toPurchaseMsisdn(String msisdn) {
        return toInternationalMsisdn(msisdn);
    }

    /** International 2547… / 25411… for fetch query and x-msisdn header. */
    public static String toInternationalMsisdn(String msisdn) {
        if (msisdn == null) {
            return null;
        }
        String cleaned = msisdn.trim();
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("254") && cleaned.length() >= 12) {
            return cleaned;
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("254")) {
            return cleaned;
        }
        return "254" + cleaned;
    }

    /** National form (7… / 11…) for purchase JSON body sample. */
    public static String toNationalMsisdn(String msisdn) {
        if (msisdn == null) {
            return null;
        }
        String cleaned = msisdn.trim();
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("254") && cleaned.length() >= 12) {
            return cleaned.substring(3);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            return cleaned.substring(1);
        }
        return cleaned;
    }

    public static String normalizePaymentMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "airtime";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
        if (normalized.equals("m-pesa") || normalized.equals("mpesa") || normalized.equals("m_pesa")) {
            // Daraja docs sample values: "Airtime/m-pesa" (hyphenated lowercase).
            return "m-pesa";
        }
        return "airtime";
    }

    private static String stripDecimals(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private static boolean isPendingStatus(String code) {
        if (code == null) {
            return true;
        }
        String hay = code.toLowerCase(Locale.ROOT);
        return hay.contains("pending") || hay.contains("processing") || hay.equals("0");
    }

    private static String mapStatusLabel(String code, String desc) {
        if (SafaricomApiErrorMapper.isSuccess(code)) {
            return "SUCCESS";
        }
        if (isPendingStatus(code)) {
            return "PENDING";
        }
        if (desc != null && desc.toLowerCase(Locale.ROOT).contains("cancel")) {
            return "CANCELLED";
        }
        return "FAILED";
    }

    private static JsonNode firstArray(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.path(key);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode()) {
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

    private static BigDecimal decimal(JsonNode node, String... keys) {
        String raw = text(node, keys);
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").replace("KES", "").trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String abbreviate(String value) {
        return abbreviate(value, 500);
    }

    private static String abbreviate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) + "…" : value;
    }

    public record PurchaseCommand(
            String msisdn,
            String offeringId,
            String accountId,
            BigDecimal price,
            String resourceAmount,
            String validity,
            String transactionId,
            String paymentMode,
            String paymentMsisdn) {
    }

    public record PurchaseResult(
            String responseCode,
            String responseDescription,
            String checkoutRequestId,
            String providerRequestId) {
    }

    public record StatusResult(String status, String responseCode, String responseDescription) {
    }
}
