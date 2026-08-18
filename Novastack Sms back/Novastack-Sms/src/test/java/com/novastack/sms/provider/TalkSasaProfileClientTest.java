package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.TalkSasaAccountResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkSasaProfileClientTest {

    private MockWebServer server;
    private AppProperties properties;
    private TalkSasaProfileClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        properties = new AppProperties();
        properties.getSms().getTalksasa().setApiToken("test-token");
        properties.getSms().getTalksasa().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getSms().getTalksasa().setReadTimeoutMs(2_000);
        client = newClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getAccountReadsProfileAndBalance() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"name":"Nova Ops","email":"ops@example.com","status":"active"}}
                """);
        enqueueSuccess("""
                {"status":"success","data":{
                  "remaining_units": 1200,
                  "used_units": 300,
                  "total_units": 1500,
                  "unit_type": "SMS"
                }}
                """);

        TalkSasaAccountResponse account = client.getAccount();

        assertTrue(account.isConfigured());
        assertTrue(account.isReachable());
        assertEquals("Nova Ops", account.getProfile().getName());
        assertEquals("ops@example.com", account.getProfile().getEmail());
        assertEquals(new BigDecimal("1200"), account.getBalance().getRemainingUnits());
        assertEquals(new BigDecimal("300"), account.getBalance().getUsedUnits());

        RecordedRequest me = takeRequest();
        assertEquals("GET", me.getMethod());
        assertEquals("/me", me.getPath());
        assertEquals("Bearer test-token", me.getHeader("Authorization"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, me.getHeader("Accept"));

        RecordedRequest balance = takeRequest();
        assertEquals("GET", balance.getMethod());
        assertEquals("/balance", balance.getPath());
        assertEquals("Bearer test-token", balance.getHeader("Authorization"));
        assertFalse(me.getHeader("Authorization").contains("49|"));
    }

    @Test
    void getAccountParsesRemainingBalanceString() {
        enqueueSuccess("""
                {"status":"success","data":{"first_name":"Ken","last_name":"Ops"}}
                """);
        enqueueSuccess("""
                {"status":"success","data":{"remaining_balance":"88.500","expired_on":null}}
                """);

        TalkSasaAccountResponse account = client.getAccount();

        assertEquals("Ken Ops", account.getProfile().getName());
        assertEquals(new BigDecimal("88.500"), account.getBalance().getRemainingUnits());
        assertNull(account.getBalance().getExpiredOn());
    }

    @Test
    void authenticationFailureIsNotReachable() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"status\":\"error\",\"message\":\"Unauthorized\"}"));
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"status\":\"error\",\"message\":\"Unauthorized\"}"));

        TalkSasaAccountResponse account = client.getAccount();

        assertTrue(account.isConfigured());
        assertFalse(account.isReachable());
        assertEquals(ProviderErrorMessages.AUTH, account.getErrorMessage());
        assertNull(account.getProfile());
        assertNull(account.getBalance());
    }

    @Test
    void missingTokenDoesNotCallProvider() {
        properties.getSms().getTalksasa().setApiToken(" ");
        client = newClient();

        TalkSasaAccountResponse account = client.getAccount();

        assertFalse(account.isConfigured());
        assertFalse(account.isReachable());
        assertEquals(ProviderErrorMessages.NOT_CONFIGURED, account.getErrorMessage());
        assertEquals(0, server.getRequestCount());
        assertNotNull(account);
    }

    private TalkSasaProfileClient newClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getSms().getTalksasa().getBaseUrl())
                .build();
        return new TalkSasaProfileClient(properties, new ObjectMapper(), webClient);
    }

    private void enqueueSuccess(String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("No TalkSasa request recorded");
        }
        return request;
    }
}
