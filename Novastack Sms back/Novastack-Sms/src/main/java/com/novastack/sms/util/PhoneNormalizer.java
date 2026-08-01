package com.novastack.sms.util;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static boolean looksLikePhone(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String cleaned = value.replaceAll("[\\s\\-()+]", "");
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        return cleaned.matches("\\d{9,15}") && !value.contains("@");
    }

    public static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    /** Normalize Kenyan-style numbers to 254… (supports 07… and 011…). */
    public static String normalize(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            // 07XXXXXXXX → 2547XXXXXXXX, 011XXXXXXX → 25411XXXXXXX
            cleaned = "254" + cleaned.substring(1);
        } else if (cleaned.matches("^7\\d{8}$") || cleaned.matches("^11\\d{7}$")) {
            cleaned = "254" + cleaned;
        }
        return cleaned;
    }

    /** Safaricom MSISDN in international form: 2547XXXXXXXX or 25411XXXXXXX. */
    public static boolean isSafaricomMsisdn(String phone) {
        return phone != null && phone.matches("^254(7\\d{8}|11\\d{7})$");
    }
}
