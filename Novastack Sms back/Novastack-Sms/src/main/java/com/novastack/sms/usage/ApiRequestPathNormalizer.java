package com.novastack.sms.usage;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes request paths so UUIDs and numeric ids collapse to {id} for analytics.
 */
public final class ApiRequestPathNormalizer {

    private static final Pattern UUID = Pattern.compile(
            "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern LONG_ID = Pattern.compile("/\\d{6,}(?=/|$)");

    private ApiRequestPathNormalizer() {
    }

    public static String normalize(String uri) {
        if (uri == null || uri.isBlank()) {
            return "/";
        }
        int q = uri.indexOf('?');
        String path = q >= 0 ? uri.substring(0, q) : uri;
        int api = path.indexOf("/api/v1/");
        if (api >= 0) {
            path = path.substring(api);
        }
        path = UUID.matcher(path).replaceAll("/{id}");
        path = LONG_ID.matcher(path).replaceAll("/{id}");
        if (path.length() > 180) {
            path = path.substring(0, 180);
        }
        return path;
    }

    public static String category(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (value.startsWith("/api/v1/sms")) {
            return "SMS";
        }
        if (value.startsWith("/api/v1/mpesa")) {
            return "MPESA";
        }
        if (value.startsWith("/api/v1/wallet")) {
            return "WALLET";
        }
        return "OTHER";
    }
}
