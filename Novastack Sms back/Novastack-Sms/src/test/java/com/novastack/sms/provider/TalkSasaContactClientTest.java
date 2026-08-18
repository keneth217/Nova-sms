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

class TalkSasaContactClientTest {

    private MockWebServer server;
    private AppProperties properties;
    private TalkSasaContactClient client;

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
    void storePostsPhoneAndNames() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"606732aec8705","phone":"254712345678","first_name":"Jane","last_name":"Doe"}}
                """);

        Optional<TalkSasaContactClient.TalkSasaContact> result =
                client.store("6065ecdc9184a", "254712345678", "Jane", "Doe");

        assertTrue(result.isPresent());
        assertEquals("606732aec8705", result.get().uid());
        assertEquals("254712345678", result.get().phone());

        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/store", request.getPath());
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Accept"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"phone\":\"254712345678\""));
        assertTrue(body.contains("\"first_name\":\"Jane\""));
        assertTrue(body.contains("\"last_name\":\"Doe\""));
        assertFalse(request.getHeader("Authorization").contains("49|"));
    }

    @Test
    void searchUsesPost() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"606732aec8705","phone":"254712345678"}}
                """);

        Optional<TalkSasaContactClient.TalkSasaContact> result =
                client.get("6065ecdc9184a", "606732aec8705");

        assertTrue(result.isPresent());
        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/search/606732aec8705", request.getPath());
    }

    @Test
    void updatePatchesPhone() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"606732aec8705","phone":"254722000111"}}
                """);

        Optional<TalkSasaContactClient.TalkSasaContact> result =
                client.update("6065ecdc9184a", "606732aec8705", "254722000111", null, null);

        assertTrue(result.isPresent());
        assertEquals("254722000111", result.get().phone());
        RecordedRequest request = takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/update/606732aec8705", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"phone\":\"254722000111\""));
        assertFalse(body.contains("first_name"));
    }

    @Test
    void deleteSucceedsWhenDataIsNull() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":null}
                """);

        assertTrue(client.delete("6065ecdc9184a", "606732aec8705"));
        RecordedRequest request = takeRequest();
        assertEquals("DELETE", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/delete/606732aec8705", request.getPath());
    }

    @Test
    void listParsesPaginatedContacts() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"data":[
                  {"uid":"c1","phone":"254711111111"},
                  {"uid":"c2","phone":"254722222222"}
                ]}}
                """);

        List<TalkSasaContactClient.TalkSasaContact> contacts = client.list("6065ecdc9184a");

        assertEquals(2, contacts.size());
        assertEquals("c1", contacts.get(0).uid());
        assertEquals("c2", contacts.get(1).uid());
        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/contacts/6065ecdc9184a/all", request.getPath());
    }

    @Test
    void authenticationFailureReturnsEmpty() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"status\":\"error\",\"message\":\"Unauthorized\"}"));

        Optional<TalkSasaContactClient.TalkSasaContact> result =
                client.store("6065ecdc9184a", "254712345678", "Jane", "Doe");

        assertTrue(result.isEmpty());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void missingTokenDoesNotCallProvider() {
        properties.getSms().getTalksasa().setApiToken(" ");
        client = newClient();

        assertFalse(client.isEnabled());
        assertTrue(client.store("g1", "254712345678", null, null).isEmpty());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void syncDisabledDoesNotCallProvider() {
        properties.getSms().getTalksasa().setSyncContactGroups(false);
        client = newClient();

        assertFalse(client.isEnabled());
        assertTrue(client.store("g1", "254712345678", null, null).isEmpty());
        assertEquals(0, server.getRequestCount());
    }

    private TalkSasaContactClient newClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getSms().getTalksasa().getBaseUrl())
                .build();
        return new TalkSasaContactClient(properties, new ObjectMapper(), webClient);
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
