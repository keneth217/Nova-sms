package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.enums.MessageStatus;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkSasaSmsProviderTest {

    private MockWebServer server;
    private TalkSasaSmsProvider provider;
    private AppProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        properties = new AppProperties();
        properties.getSms().getTalksasa().setApiToken("test-token");
        properties.getSms().getTalksasa().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getSms().getTalksasa().setReadTimeoutMs(2_000);
        WebClient client = WebClient.builder()
                .baseUrl(properties.getSms().getTalksasa().getBaseUrl())
                .build();
        provider = new TalkSasaSmsProvider(properties, objectMapper, client);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void sendSuccessStoresUidAndDoesNotLogTokenInBody() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"abc123uid","recipient":"254712345678","status":"pending"}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertTrue(result.success());
        assertEquals("abc123uid", result.providerMessageId());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());

        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/sms/send", request.getPath());
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("254712345678"));
        assertTrue(body.contains("\"type\":\"plain\""));
        assertFalse(result.rawRequest().toLowerCase().contains("bearer"));
    }

    @Test
    void sendWhatsAppUsesWhatsappType() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"wa123uid","recipient":"254712345678","status":"pending"}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                null, null, "254712345678", "Hello on WhatsApp", "TALK_SASA", null, "whatsapp"));

        assertTrue(result.success());
        assertEquals("wa123uid", result.providerMessageId());
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/sms/send", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"type\":\"whatsapp\""));
        assertTrue(body.contains("Hello on WhatsApp"));
        assertFalse(body.toLowerCase().contains("bearer"));
    }

    @Test
    void authenticationFailureIsNotRetryable() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"status\":\"error\",\"message\":\"Unauthorized\"}"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertEquals(ProviderErrorMessages.AUTH, result.errorMessage());
        assertNull(result.providerMessageId());
    }

    @Test
    void validationErrorIsNotRetryable() {
        server.enqueue(new MockResponse().setResponseCode(422).setBody("{\"status\":\"error\",\"message\":\"invalid sender\"}"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertEquals("Invalid sender.", result.errorMessage());
    }

    @Test
    void unauthorizedOriginatorIsMappedAsInvalidSender() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"status\":\"error\",\"message\":\"Originator TALK_SASA is not authorized to send this message\"}"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertEquals("Sender ID TALK_SASA is not authorized to send this message.", result.errorMessage());
    }

    @Test
    void rateLimitIsNotRetriedAutomatically() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"status\":\"error\"}"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertEquals(ProviderErrorMessages.RATE_LIMIT, result.errorMessage());
    }

    @Test
    void serverErrorIsRetryable() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("unavailable"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK_SASA"));

        assertFalse(result.success());
        assertTrue(result.retryable());
        assertEquals(ProviderErrorMessages.UNAVAILABLE, result.errorMessage());
    }

    @Test
    void successWithoutUidIsAccepted() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"from":"TALK-SASA","to":"254712345678","message":"Hello","sms_count":1}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());
        assertNull(result.providerMessageId());
    }

    @Test
    void http200WithoutStatusIsAcceptedForRequestedRecipient() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{}"));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());
    }

    @Test
    void reportPhoneMismatchStillAcceptsRequestedRecipient() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"u9","to":"0711766223"}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254711766223", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals("u9", result.providerMessageId());
    }

    @Test
    void successWithStringDataIsAccepted() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":"sms reports with all details"}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());
    }

    @Test
    void successWithJsonStringDataStoresUid() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":"{\\"uid\\":\\"606812e63f78b\\",\\"status\\":\\"pending\\"}"}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals("606812e63f78b", result.providerMessageId());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());
    }

    @Test
    void messageidFieldIsStoredAsUid() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"messageid":"abc123uid","to":"254712345678"}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Hello", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals("abc123uid", result.providerMessageId());
    }

    @Test
    void queueUidFromTalkSasaSendIsStored() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","message":"Your SMS is being processed and will be delivered","data":{"queue_uid":"f055e508-bc58-4cec-bfbc-009754872c78","status":"accepted","recipients_count":1,"sms_count":1,"estimated_cost":1,"check_status_url":"https://bulksms.talksasa.com/api/v3/sms/queue/f055e508-bc58-4cec-bfbc-009754872c78"}}
                        """));

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254711766223", "Hello from the Nova SMS test console.", "TALK-SASA"));

        assertTrue(result.success());
        assertEquals("f055e508-bc58-4cec-bfbc-009754872c78", result.providerMessageId());
        assertEquals(MessageStatus.ACCEPTED, result.mappedStatus());
    }

    @Test
    void getSmsStatusMapsProductionCompletedQueueToDelivered() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","message":null,"data":{"queue_uid":"58ef3d58-77ae-4dcb-b633-ee9a02a26c9f","status":"completed","recipient_count":1,"processed_count":1,"failed_count":0,"remaining":0,"total_cost":1,"created_at":"2026-08-18T14:45:41.000000Z","completed_at":"2026-08-18T14:45:41.000000Z","error":null}}
                        """));

        SmsProvider.SmsStatusResult result = provider.getSmsStatus("58ef3d58-77ae-4dcb-b633-ee9a02a26c9f");
        assertTrue(result.success());
        assertEquals(MessageStatus.DELIVERED, result.status());
        assertEquals("completed", result.providerStatus());
        assertEquals(1, result.processedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.remaining());
        assertEquals(Instant.parse("2026-08-18T14:45:41.000000Z"), result.occurredAt());
        assertEquals("/sms/queue/58ef3d58-77ae-4dcb-b633-ee9a02a26c9f", server.takeRequest(2, TimeUnit.SECONDS).getPath());
    }

    @Test
    void getSmsStatusUsesQueuePathForUuid() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"queue_uid":"f055e508-bc58-4cec-bfbc-009754872c78","status":"sent"}}
                        """));

        SmsProvider.SmsStatusResult result = provider.getSmsStatus("f055e508-bc58-4cec-bfbc-009754872c78");
        assertTrue(result.success());
        assertEquals(MessageStatus.SENT, result.status());
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/sms/queue/f055e508-bc58-4cec-bfbc-009754872c78", request.getPath());
    }

    @Test
    void getSmsStatusFallsBackFromQueueToSmsUid() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"status\":\"error\",\"message\":\"Not found\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"f055e508-bc58-4cec-bfbc-009754872c78","status":"delivered"}}
                        """));

        SmsProvider.SmsStatusResult result = provider.getSmsStatus("f055e508-bc58-4cec-bfbc-009754872c78");
        assertTrue(result.success());
        assertEquals(MessageStatus.DELIVERED, result.status());
        assertEquals("/sms/queue/f055e508-bc58-4cec-bfbc-009754872c78", server.takeRequest(2, TimeUnit.SECONDS).getPath());
        assertEquals("/sms/f055e508-bc58-4cec-bfbc-009754872c78", server.takeRequest(2, TimeUnit.SECONDS).getPath());
    }

    @Test
    void getSmsStatusMapsDelivered() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"abc123uid","status":"delivered"}}
                        """));

        SmsProvider.SmsStatusResult result = provider.getSmsStatus("abc123uid");
        assertTrue(result.success());
        assertEquals(MessageStatus.DELIVERED, result.status());
        assertEquals("abc123uid", result.providerMessageId());
    }

    @Test
    void sendBulkJoinsRecipients() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":[
                          {"uid":"u1","recipient":"254712345678","status":"sent"},
                          {"uid":"u2","recipient":"254707711847","status":"sent"}
                        ]}
                        """));

        Map<String, SmsProvider.SmsProviderResult> results = provider.sendBulk(new SmsProvider.SmsBulkRequest(
                List.of("254712345678", "254707711847"), "Hi", "TALK_SASA"));

        assertEquals("u1", results.get("254712345678").providerMessageId());
        assertEquals("u2", results.get("254707711847").providerMessageId());
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertTrue(request.getBody().readUtf8().contains("254712345678,254707711847"));
    }

    @Test
    void omittedSenderIdUsesTalkSasaDefault() throws Exception {
        enqueueAccepted();

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Test message", null));

        assertTrue(result.success());
        JsonNode body = recordedSendBody();
        assertEquals("254712345678", body.get("recipient").asText());
        assertEquals("TALK-SASA", body.get("sender_id").asText());
        assertEquals("plain", body.get("type").asText());
        assertEquals("Test message", body.get("message").asText());
    }

    @Test
    void underscoreTalkSasaAliasIsSentAsHyphenatedDefault() throws Exception {
        enqueueAccepted();

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Test message", "TALK_SASA"));

        assertTrue(result.success());
        assertEquals("TALK-SASA", recordedSendBody().get("sender_id").asText());
    }

    @Test
    void explicitSenderIdIsForwardedToTalkSasa() throws Exception {
        enqueueAccepted();

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Test message", "MYCOMPANY"));

        assertTrue(result.success());
        JsonNode body = recordedSendBody();
        assertEquals("MYCOMPANY", body.get("sender_id").asText());
        assertEquals("254712345678", body.get("recipient").asText());
        assertEquals("plain", body.get("type").asText());
        assertEquals("Test message", body.get("message").asText());
    }

    @Test
    void configuredTalkSasaSenderOverrideIsUsedWhenOmitted() throws Exception {
        properties.getSms().getTalksasa().setDefaultSenderId("MYTEST");
        enqueueAccepted();

        SmsProvider.SmsProviderResult result = provider.send(new SmsProvider.SmsProviderRequest(
                "254712345678", "Test message", null));

        assertTrue(result.success());
        assertEquals("MYTEST", recordedSendBody().get("sender_id").asText());
    }

    private void enqueueAccepted() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"status":"success","data":{"uid":"abc123uid","recipient":"254712345678","status":"pending"}}
                        """));
    }

    private JsonNode recordedSendBody() throws Exception {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertEquals("/sms/send", request.getPath());
        return objectMapper.readTree(request.getBody().readUtf8());
    }
}
