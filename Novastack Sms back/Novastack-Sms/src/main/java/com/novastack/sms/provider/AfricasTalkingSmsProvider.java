package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Africa's Talking Bulk SMS API client.
 * Live: https://api.africastalking.com/version1/messaging
 * Production: https://api.africastalking.com/version1/messaging
 * Sandbox: https://api.sandbox.africastalking.com/version1/messaging (username must be "sandbox")
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AfricasTalkingSmsProvider implements SmsProvider {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String getName() {
        return "AFRICAS_TALKING";
    }

    @Override
    public SmsProviderResult send(SmsProviderRequest request) {
        Map<String, SmsProviderResult> results = sendBulk(new SmsBulkRequest(
                request.username(),
                request.apiKey(),
                java.util.List.of(request.recipient()),
                request.message(),
                request.senderId(),
                request.baseUrl()
        ));
        return results.values().stream().findFirst()
                .orElse(SmsProviderResult.failure(null, null, null, "No Africa's Talking response"));
    }

    @Override
    public Map<String, SmsProviderResult> sendBulk(SmsBulkRequest request) {
        Map<String, SmsProviderResult> byRecipient = new LinkedHashMap<>();
        Collection<String> recipients = request.recipients() == null
                ? java.util.List.of()
                : request.recipients().stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (recipients.isEmpty()) {
            return byRecipient;
        }

        Credentials credentials = resolveCredentials(request.username(), request.apiKey(), request.baseUrl());
        if (credentials.error() != null) {
            for (String recipient : recipients) {
                byRecipient.put(normalizePhoneKey(recipient),
                        SmsProviderResult.failure("missing-credentials", null, 401, credentials.error()));
            }
            return byRecipient;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", credentials.username());
        form.add("to", String.join(",", recipients));
        form.add("message", request.message());
        // Required for bulk MT shortcode billing semantics; ignored for alphanumerics.
        form.add("bulkSMSMode", "1");
        // Queue at AT and acknowledge quickly.
        form.add("enqueue", "1");

        // Sandbox often rejects custom sender IDs; omit "from" so AT uses AFRICASTKNG.
        if (!"sandbox".equalsIgnoreCase(credentials.username())
                && request.senderId() != null
                && !request.senderId().isBlank()) {
            form.add("from", request.senderId());
        }

        String rawRequest = form.toString();
        log.info("AT SMS request username={} recipients={} from={} enqueue=1 bulkSMSMode=1",
                credentials.username(),
                recipients.size(),
                form.containsKey("from") ? form.getFirst("from") : "(default)");

        try {
            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(credentials.baseUrl() + appProperties.getAfricastalking().getMessagingPath())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("apiKey", credentials.apiKey())
                    .body(form)
                    .retrieve()
                    .body(String.class);

            return parseRecipientResults(recipients, rawRequest, responseBody);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("AT SMS HTTP error status={} body={}", status, ex.getResponseBodyAsString());
            String error = extractHttpError(ex);
            boolean retryable = ProviderErrorMessages.isRetryable(status, false);
            for (String recipient : recipients) {
                byRecipient.put(normalizePhoneKey(recipient),
                        SmsProviderResult.failure(rawRequest, ex.getResponseBodyAsString(),
                                status, customerAtError(status, error), retryable));
            }
            return byRecipient;
        } catch (Exception ex) {
            log.error("AT SMS send failed: {}", ex.getMessage());
            boolean retryable = ProviderErrorMessages.isRetryable(null, true);
            for (String recipient : recipients) {
                byRecipient.put(normalizePhoneKey(recipient),
                        SmsProviderResult.failure(rawRequest, null, null, ProviderErrorMessages.UNAVAILABLE, retryable));
            }
            return byRecipient;
        }
    }

    private Map<String, SmsProviderResult> parseRecipientResults(
            Collection<String> requested,
            String rawRequest,
            String responseBody) {
        Map<String, SmsProviderResult> byRecipient = new LinkedHashMap<>();
        for (String recipient : requested) {
            byRecipient.put(normalizePhoneKey(recipient),
                    SmsProviderResult.failure(rawRequest, responseBody, 200, "Recipient missing from Africa's Talking response"));
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode recipients = root.path("SMSMessageData").path("Recipients");
            if (!recipients.isArray() || recipients.isEmpty()) {
                String message = root.path("SMSMessageData").path("Message").asText("Unknown Africa's Talking response");
                for (String key : byRecipient.keySet()) {
                    byRecipient.put(key, SmsProviderResult.failure(rawRequest, responseBody, 200, message));
                }
                return byRecipient;
            }

            for (JsonNode item : recipients) {
                String number = item.path("number").asText("");
                String key = normalizePhoneKey(number);
                int statusCode = item.path("statusCode").asInt(-1);
                String status = item.path("status").asText("");
                String messageId = blankToNull(item.path("messageId").asText(null));
                String cost = item.path("cost").asText(null);

                if (isAccepted(statusCode, status)) {
                    byRecipient.put(key, SmsProviderResult.accepted(messageId, rawRequest, responseBody, statusCode));
                } else {
                    String error = describeStatus(statusCode, status, cost);
                    byRecipient.put(key, SmsProviderResult.failure(rawRequest, responseBody, statusCode, error, false));
                }
            }
            return byRecipient;
        } catch (Exception ex) {
            for (String key : byRecipient.keySet()) {
                byRecipient.put(key, SmsProviderResult.failure(rawRequest, responseBody, 200,
                        "Failed to parse Africa's Talking response"));
            }
            return byRecipient;
        }
    }

    private boolean isAccepted(int statusCode, String status) {
        // 100 Processed, 101 Sent, 102 Queued
        if (statusCode == 100 || statusCode == 101 || statusCode == 102) {
            return true;
        }
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.equals("success")
                || normalized.equals("sent")
                || normalized.equals("queued")
                || normalized.equals("processed");
    }

    private String describeStatus(int statusCode, String status, String cost) {
        String mapped = switch (statusCode) {
            case 401 -> "RiskHold";
            case 402 -> "InvalidSenderId";
            case 403 -> "InvalidPhoneNumber";
            case 404 -> "UnsupportedNumberType";
            case 405 -> "InsufficientBalance";
            case 406 -> "UserInBlacklist";
            case 407 -> "CouldNotRoute";
            case 409 -> "DoNotDisturbRejection";
            case 500 -> "InternalServerError";
            case 501 -> "GatewayError";
            case 502 -> "RejectedByGateway";
            default -> null;
        };
        StringBuilder sb = new StringBuilder();
        if (mapped != null) {
            sb.append(mapped);
        } else if (status != null && !status.isBlank()) {
            sb.append(status);
        } else {
            sb.append("Africa's Talking rejected the message");
        }
        if (statusCode > 0) {
            sb.append(" (statusCode ").append(statusCode).append(')');
        }
        if (cost != null && !cost.isBlank()) {
            sb.append(" · cost ").append(cost);
        }
        return sb.toString();
    }

    private Credentials resolveCredentials(String username, String apiKey, String baseUrl) {
        String resolvedUsername = blankToNull(username) != null
                ? username
                : appProperties.getAfricastalking().getUsername();
        String resolvedApiKey = blankToNull(apiKey) != null
                ? apiKey
                : appProperties.getAfricastalking().getApiKey();
        String resolvedBaseUrl = blankToNull(baseUrl) != null
                ? baseUrl
                : appProperties.getAfricastalking().getBaseUrl();

        if (resolvedUsername == null || resolvedUsername.isBlank()
                || resolvedApiKey == null || resolvedApiKey.isBlank()) {
            return new Credentials(null, null, null,
                    "Africa's Talking is not configured. Set AT_USERNAME and AT_API_KEY (use username 'sandbox' for sandbox testing).");
        }

        if (!"sandbox".equalsIgnoreCase(resolvedUsername)
                && resolvedBaseUrl != null
                && resolvedBaseUrl.contains("sandbox.africastalking.com")) {
            resolvedBaseUrl = "https://api.africastalking.com";
        }
        if ("sandbox".equalsIgnoreCase(resolvedUsername)
                && resolvedBaseUrl != null
                && !resolvedBaseUrl.contains("sandbox")) {
            resolvedBaseUrl = "https://api.sandbox.africastalking.com";
        }

        return new Credentials(resolvedUsername, resolvedApiKey, resolvedBaseUrl, null);
    }

    private String customerAtError(int status, String vendorMessage) {
        if (status == 401 || status == 403) {
            return ProviderErrorMessages.forHttpStatus(status);
        }
        if (status >= 500) {
            return ProviderErrorMessages.UNAVAILABLE;
        }
        return vendorMessage != null && !vendorMessage.isBlank()
                ? vendorMessage
                : ProviderErrorMessages.forHttpStatus(status);
    }

    private String extractHttpError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                String message = root.path("SMSMessageData").path("Message").asText(null);
                if (message != null && !message.isBlank()) {
                    return message;
                }
            } catch (Exception ignored) {
                // fall through
            }
            return body.length() > 300 ? body.substring(0, 300) + "…" : body;
        }
        return ex.getStatusCode() + " " + ex.getStatusText();
    }

    private String normalizePhoneKey(String phone) {
        if (phone == null) {
            return "";
        }
        String cleaned = phone.trim().replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() || "None".equalsIgnoreCase(value) ? null : value;
    }

    private record Credentials(String username, String apiKey, String baseUrl, String error) {
    }
}
