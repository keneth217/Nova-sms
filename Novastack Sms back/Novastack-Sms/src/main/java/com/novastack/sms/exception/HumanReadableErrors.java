package com.novastack.sms.exception;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HumanReadableErrors {

    public static final String GENERIC = "Something went wrong. Please try again.";

    private static final Pattern JSON_MESSAGE = Pattern.compile(
            "\"(?:message|error|error_message)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE);

    private HumanReadableErrors() {
    }

    public static String fromException(Throwable ex) {
        if (ex == null) {
            return GENERIC;
        }
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ApiException api && hasText(api.getMessage())) {
                return api.getMessage().trim();
            }
        }
        String raw = mostSpecificMessage(ex);
        String human = humanize(raw);
        return human != null ? human : GENERIC;
    }

    public static String fromVendor(String detail) {
        String extracted = extractJsonMessage(detail);
        return humanize(extracted != null ? extracted : detail);
    }

    public static String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.replace('\n', ' ').replace('\r', ' ').trim();
        text = text.replaceAll("(?i)Bearer\\s+\\S+", "Bearer ***");
        text = text.replaceAll("(?i)(api[_-]?token\"?\\s*[:=]\\s*\")[^\"]+", "$1***");
        if (isInternal(text)) {
            return null;
        }
        if (text.length() < 8 || text.length() > 240) {
            return null;
        }
        if (!text.contains(" ") && !text.contains(".")) {
            return null;
        }
        if (text.startsWith("{") || text.startsWith("[")) {
            return null;
        }
        text = text.replaceAll("(?i)\\boriginator\\b", "Sender ID");
        text = collapseSpaces(text);
        text = capitalize(text);
        if (!text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            text = text + ".";
        }
        return text;
    }

    private static String extractJsonMessage(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        Matcher matcher = JSON_MESSAGE.matcher(trimmed);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\"", "\"");
    }

    private static String mostSpecificMessage(Throwable ex) {
        Throwable current = ex;
        String last = null;
        while (current != null) {
            if (hasText(current.getMessage())) {
                last = current.getMessage();
            }
            current = current.getCause();
        }
        return last;
    }

    private static boolean isInternal(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return containsAny(lower,
                "could not execute",
                "sqlsyntaxerrorexception",
                "sqlexception",
                "jdbc",
                "hibernate",
                "constraint [",
                "duplicate entry",
                "nested exception",
                "failed to convert",
                "no value present",
                "stacktrace",
                " at org.",
                " at com.",
                "select ",
                "insert into",
                "update ",
                "delete from",
                "connection refused",
                "unknown column",
                "table '",
                "json parse",
                "cannot deserialize",
                "www-authenticate");
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String collapseSpaces(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
