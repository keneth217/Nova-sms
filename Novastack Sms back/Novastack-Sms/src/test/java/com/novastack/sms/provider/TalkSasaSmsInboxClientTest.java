package com.novastack.sms.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.TalkSasaSmsListResponse;
import com.novastack.sms.dto.response.TalkSasaSmsViewResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkSasaSmsInboxClientTest {

    private MockWebServer server;
    private TalkSasaSmsInboxClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        AppProperties properties = new AppProperties();
        properties.getSms().getTalksasa().setApiToken("test-token");
        properties.getSms().getTalksasa().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getSms().getTalksasa().setReadTimeoutMs(2_000);
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getSms().getTalksasa().getBaseUrl())
                .build();
        client = new TalkSasaSmsInboxClient(properties, new ObjectMapper(), webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void listReadsPaginatedSms() throws Exception {
        enqueueSuccess("""
                {"status":"success","current_page":1,"per_page":15,"total":2,"last_page":1,"data":[
                  {"uid":"606812e63f78b","to":"254712345678","from":"TALK-SASA","message":"Hello","status":"Delivered","sms_count":1,"cost":"1","created_at":"2026-08-20 10:00:00"},
                  {"uid":"606812e63f78c","to":"254711766223","from":"NOVA","message":"Top up","status":"Sent"}
                ]}
                """);

        TalkSasaSmsListResponse list = client.list(1, 15);

        assertTrue(list.isConfigured());
        assertTrue(list.isReachable());
        assertEquals(1, list.getPage());
        assertEquals(15, list.getPerPage());
        assertEquals(2L, list.getTotal());
        assertEquals(2, list.getItems().size());
        assertEquals("606812e63f78b", list.getItems().get(0).getUid());
        assertEquals("254712345678", list.getItems().get(0).getRecipient());
        assertEquals("TALK-SASA", list.getItems().get(0).getSenderId());
        assertEquals("Delivered", list.getItems().get(0).getStatus());

        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertTrue(request.getPath().startsWith("/sms?"));
        assertTrue(request.getPath().contains("page=1"));
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        assertFalse(request.getHeader("Authorization").contains("49|"));
    }

    @Test
    void getReadsSmsByUid() throws Exception {
        enqueueSuccess("""
                {"status":"success","data":{"uid":"606812e63f78b","to":"254712345678","from":"TALK-SASA","message":"Hello","status":"Delivered"}}
                """);

        TalkSasaSmsViewResponse view = client.get("606812e63f78b");

        assertTrue(view.isConfigured());
        assertTrue(view.isReachable());
        assertNotNull(view.getItem());
        assertEquals("606812e63f78b", view.getItem().getUid());
        assertEquals("254712345678", view.getItem().getRecipient());
        assertEquals("/sms/606812e63f78b", takeRequest().getPath());
    }

    @Test
    void getUsesQueuePathForUuidThenFallsBack() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"status\":\"error\",\"message\":\"Not found\"}"));
        enqueueSuccess("""
                {"status":"success","data":{"uid":"f055e508-bc58-4cec-bfbc-009754872c78","status":"delivered","to":"254712345678"}}
                """);

        TalkSasaSmsViewResponse view = client.get("f055e508-bc58-4cec-bfbc-009754872c78");

        assertTrue(view.isReachable());
        assertEquals("delivered", view.getItem().getStatus());
        assertEquals("/sms/queue/f055e508-bc58-4cec-bfbc-009754872c78", takeRequest().getPath());
        assertEquals("/sms/f055e508-bc58-4cec-bfbc-009754872c78", takeRequest().getPath());
    }

    private void enqueueSuccess(String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        return request;
    }
}
