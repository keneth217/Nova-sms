package com.novastack.sms.usage;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Masks Kenyan MSISDNs for logs. Never log API keys, PINs, or full secret bodies. */
public final class SensitiveDataMasker {

    private static final Pattern MSISDN = Pattern.compile("(?:254|0)\\d{9}");

    private SensitiveDataMasker() {
    }

    public static String maskMsisdn(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return "***";
        }
        return digits.substring(0, Math.min(6, digits.length() - 3)) + "****"
                + digits.substring(digits.length() - 2);
    }

    public static String maskText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        Matcher matcher = MSISDN.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(maskMsisdn(matcher.group())));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    public static String clientIp(String forwardedFor, String remoteAddr) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return clip(first, 64);
            }
        }
        return clip(remoteAddr, 64);
    }

    public static String userAgent(String header) {
        return clip(header, 180);
    }

    public static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
