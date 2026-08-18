package com.novastack.sms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyHasherTest {

    @Test
    void generateLiveKeyUsesPrefixAndIsUnique() {
        String first = ApiKeyHasher.generateLiveKey();
        String second = ApiKeyHasher.generateLiveKey();
        assertTrue(first.startsWith("nova_live_"));
        assertTrue(ApiKeyHasher.looksLikeNovaLiveKey(first));
        assertNotEquals(first, second);
        assertEquals(20, ApiKeyHasher.prefixOf(first).length());
    }

    @Test
    void sha256IsStableAndDoesNotContainPlaintext() {
        String key = "nova_live_abc";
        String hash = ApiKeyHasher.sha256Hex(key);
        assertEquals(64, hash.length());
        assertFalse(hash.contains("nova_live"));
        assertEquals(hash, ApiKeyHasher.sha256Hex(key));
    }

    @Test
    void legacyOrgKeyIsNotTreatedAsNovaLive() {
        assertFalse(ApiKeyHasher.looksLikeNovaLiveKey("nsk_abc"));
        assertFalse(ApiKeyHasher.looksLikeNovaLiveKey(""));
    }
}
