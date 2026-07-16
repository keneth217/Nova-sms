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
        String username = request.username() != null && !request.username().isBlank()
                ? request.username()
                : appProperties.getAfricastalking().getUsername();
        String apiKey = request.apiKey() != null && !request.apiKey().isBlank()
                ? request.apiKey()
                : appProperties.getAfricastalking().getApiKey();
        String baseUrl = request.baseUrl() != null && !request.baseUrl().isBlank()
                ? request.baseUrl()
                : appProperties.getAfricastalking().getBaseUrl();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("to", request.recipient());
        form.add("message", request.message());
        if (request.senderId() != null && !request.senderId().isBlank()) {
            form.add("from", request.senderId());
        }

        String rawRequest = form.toString();
        log.info("AT SMS request to={} from={}", request.recipient(), request.senderId());

        try {
            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(baseUrl + appProperties.getAfricastalking().getMessagingPath())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("apiKey", apiKey)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            return parseSuccess(rawRequest, responseBody);
        } catch (RestClientResponseException ex) {
            log.error("AT SMS HTTP error status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            return SmsProviderResult.failure(rawRequest, ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex.getMessage());
        } catch (Exception ex) {
            log.error("AT SMS send failed: {}", ex.getMessage(), ex);
            return SmsProviderResult.failure(rawRequest, null, null, ex.getMessage());
        }
    }

    private SmsProviderResult parseSuccess(String rawRequest, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode recipients = root.path("SMSMessageData").path("Recipients");
            if (recipients.isArray() && !recipients.isEmpty()) {
                JsonNode first = recipients.get(0);
                String status = first.path("status").asText("");
                String messageId = first.path("messageId").asText(null);
                boolean ok = status.equalsIgnoreCase("Success") || status.toLowerCase().contains("sent");
                if (ok) {
                    return new SmsProviderResult(true, messageId, rawRequest, responseBody, 201, null);
                }
                return SmsProviderResult.failure(rawRequest, responseBody, 201, status);
            }
            String message = root.path("SMSMessageData").path("Message").asText("Unknown AT response");
            return SmsProviderResult.failure(rawRequest, responseBody, 200, message);
        } catch (Exception ex) {
            return SmsProviderResult.failure(rawRequest, responseBody, 200, "Failed to parse AT response");
        }
    }
}
