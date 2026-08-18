# Spring Boot integration

Recommended for NovaStack services such as Mwalimu Scheme and Chamaplus.

## Configuration

```yaml
nova:
  sms:
    base-url: ${NOVA_SMS_API_URL}
    api-key: ${NOVA_SMS_API_KEY}
```

`NOVA_SMS_API_URL` is the origin only, for example `https://smsapi.novastack.co.ke`. Paths below include `/api/v1`.

Never commit `NOVA_SMS_API_KEY`. Load it from the environment or a secrets manager.

## WebClient

```java
@Bean
WebClient novaSmsWebClient(
        @Value("${nova.sms.base-url}") String baseUrl,
        @Value("${nova.sms.api-key}") String apiKey) {

    return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .defaultHeader("Accept", "application/json")
            .build();
}
```

## Service

The public envelope is `{ success, message, data }`. Map `data` to your DTO.

```java
public Mono<NovaApiResponse<SmsMessageData>> sendSms(
        String recipient,
        String message
) {
    return webClient.post()
            .uri("/api/v1/sms/send")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .bodyValue(Map.of(
                    "recipient", recipient,
                    "message", message
            ))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<NovaApiResponse<SmsMessageData>>() {});
}
```

`SmsMessageData.id` (UUID) is the Nova SMS id. Poll `GET /api/v1/sms/{id}/status` when you need a later delivery state.

Check HTTP status and `success` before treating the SMS as accepted. HTTP 402 means the organization wallet needs funding.
