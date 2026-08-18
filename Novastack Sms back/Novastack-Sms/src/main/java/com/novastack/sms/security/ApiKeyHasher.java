package com.novastack.sms.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class ApiKeyHasher {

    public static final String LIVE_PREFIX = "nova_live_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyHasher() {
    }

    public static String generateLiveKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return LIVE_PREFIX + HexFormat.of().formatHex(bytes);
    }

    public static String prefixOf(String apiKey) {
        if (apiKey == null || apiKey.length() < 20) {
            return apiKey == null ? "" : apiKey;
        }
        return apiKey.substring(0, 20);
    }

    public static String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static boolean looksLikeNovaLiveKey(String apiKey) {
        return apiKey != null && apiKey.startsWith(LIVE_PREFIX) && apiKey.length() > LIVE_PREFIX.length();
    }
}
