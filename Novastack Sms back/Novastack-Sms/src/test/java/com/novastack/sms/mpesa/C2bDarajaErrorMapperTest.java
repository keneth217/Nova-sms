package com.novastack.sms.mpesa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class C2bDarajaErrorMapperTest {

    @Test
    void alreadyRegistered() {
        String body = "{\"errorCode\":\"500.003.1001\",\"errorMessage\":\"URLs are already registered\"}";
        assertTrue(C2bDarajaErrorMapper.alreadyRegistered(body));
        assertTrue(C2bDarajaErrorMapper.message(500, body).contains("already registered"));
    }

    @Test
    void invalidToken() {
        assertTrue(C2bDarajaErrorMapper.invalidAccessToken("400.003.01 Invalid Access Token"));
        assertTrue(C2bDarajaErrorMapper.message(400, "400.003.01 Invalid Access Token").contains("token"));
    }

    @Test
    void aggregatorDuplicate() {
        String body = "Duplicate notification info, SP ID is 123, correlator is abc";
        assertTrue(C2bDarajaErrorMapper.message(500, body).contains("aggregator"));
    }

    @Test
    void spikeArrest() {
        assertTrue(C2bDarajaErrorMapper.message(500, "500.003.02 Error Occurred: Spike Arrest Violation")
                .toLowerCase().contains("spike"));
    }

    @Test
    void quota() {
        assertTrue(C2bDarajaErrorMapper.message(500, "500.003.03 Error Occurred: Quota Violation")
                .toLowerCase().contains("quota"));
    }

    @Test
    void wrongMethod() {
        assertTrue(C2bDarajaErrorMapper.message(404, "404.001.04 Invalid Authenticator Header").contains("POST"));
    }

    @Test
    void unknownIsNotAlreadyRegistered() {
        assertFalse(C2bDarajaErrorMapper.alreadyRegistered("Success"));
        assertTrue(C2bDarajaErrorMapper.message(500, "").contains("empty body"));
    }
}
