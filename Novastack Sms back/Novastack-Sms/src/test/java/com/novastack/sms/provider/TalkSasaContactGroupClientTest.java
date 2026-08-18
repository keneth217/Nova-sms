package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkSasaContactGroupClientTest {

    private MockWebServer server;
    private AppProperties properties;
    private TalkSasaContactGroupClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        properties = new AppProperties();
        properties.getSms().getTalksasa().setApiToken("test-token");
        properties.getSms().getTalksasa().setSyncContactGroups(true);
        properties.getSms().getTalksasa().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getSms().getTalksasa().setReadTimeoutMs(2_000);
        client = newClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void createSendsBearerTokenAndStoresUid() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"6065ecdc9184a","name":"Codeglen"}}
                """);

        Optional<TalkSasaContactGroupClient.TalkSasaGroup> result = client.create("Codeglen");

        assertTrue(result.isPresent());
        assertEquals("6065ecdc9184a", result.get().uid());
        assertEquals("Codeglen", result.get().name());

        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/contacts", request.getPath());
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Accept"));
        assertTrue(request.getBody().readUtf8().contains("\"name\":\"Codeglen\""));
        assertFalse(request.getHeader("Authorization").contains("49|"));
    }

    @Test
    void listParsesPaginatedGroups() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"data":[
                  {"uid":"g1","name":"Alpha"},
                  {"uid":"g2","name":"Beta"}
                ]}}
                """);

        List<TalkSasaContactGroupClient.TalkSasaGroup> groups = client.list();

        assertEquals(2, groups.size());
        assertEquals("g1", groups.get(0).uid());
        assertEquals("g2", groups.get(1).uid());
        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/contacts/", request.getPath());
    }

    @Test
    void viewUsesPostShow() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"6065ecdc9184a","name":"Codeglen"}}
                """);

        Optional<TalkSasaContactGroupClient.TalkSasaGroup> result = client.get("6065ecdc9184a");

        assertTrue(result.isPresent());
        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/show", request.getPath());
    }

    @Test
    void updatePatchesName() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"6065ecdc9184a","name":"Codeglen Update"}}
                """);

        Optional<TalkSasaContactGroupClient.TalkSasaGroup> result =
                client.update("6065ecdc9184a", "Codeglen Update");

        assertTrue(result.isPresent());
        assertEquals("Codeglen Update", result.get().name());
        RecordedRequest request = takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("Codeglen Update"));
    }

    @Test
    void deleteSucceedsWhenDataIsNull() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":null}
                """);

        assertTrue(client.delete("6065ecdc9184a"));
        RecordedRequest request = takeRequest();
        assertEquals("DELETE", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a", request.getPath());
    }

    @Test
    void authenticationFailureReturnsEmpty() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"status\":\"error\",\"message\":\"Unauthorized\"}"));

        Optional<TalkSasaContactGroupClient.TalkSasaGroup> result = client.create("Customers");

        assertTrue(result.isEmpty());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void missingTokenDoesNotCallProvider() {
        properties.getSms().getTalksasa().setApiToken(" ");
        client = newClient();

        assertFalse(client.isEnabled());
        assertTrue(client.create("Customers").isEmpty());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void syncDisabledDoesNotCallProvider() {
        properties.getSms().getTalksasa().setSyncContactGroups(false);
        client = newClient();

        assertFalse(client.isEnabled());
        assertTrue(client.create("Customers").isEmpty());
        assertEquals(0, server.getRequestCount());
    }

    private TalkSasaContactGroupClient newClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getSms().getTalksasa().getBaseUrl())
                .build();
        return new TalkSasaContactGroupClient(properties, new ObjectMapper(), webClient);
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
