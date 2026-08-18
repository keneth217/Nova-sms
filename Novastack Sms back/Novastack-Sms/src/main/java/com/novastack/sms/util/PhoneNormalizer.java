package com.novastack.sms.util;

import com.novastack.sms.exception.ApiException;
import org.springframework.http.HttpStatus;

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
        if (phone == null) {
            return "";
        }
        String cleaned = phone.replaceAll("[\\s\\-().]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            // 07XXXXXXXX → 2547XXXXXXXX, 011XXXXXXX → 25411XXXXXXX
            cleaned = "254" + cleaned.substring(1);
        } else if (cleaned.matches("^7\\d{8}$") || cleaned.matches("^11\\d{7}$") || cleaned.matches("^1\\d{8}$")) {
            cleaned = "254" + cleaned;
        }
        return cleaned;
    }

    /**
     * Lookup key used to match provider responses to stored recipients.
     * Strips +, spaces, hyphens, and a leading 00.
     */
    public static String lookupKey(String phone) {
        if (phone == null) {
            return "";
        }
        String cleaned = phone.trim().replaceAll("[\\s-]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("00")) {
            cleaned = cleaned.substring(2);
        }
        return cleaned;
    }

    /** Kenyan MSISDN in international form: 254XXXXXXXXX (12 digits). */
    public static boolean isKenyanMobile(String phone) {
        return phone != null && phone.matches("^254\\d{9}$");
    }

    /** Safaricom MSISDN in international form: 2547XXXXXXXX or 25411XXXXXXX. */
    public static boolean isSafaricomMsisdn(String phone) {
        return phone != null && phone.matches("^254(7\\d{8}|11\\d{7})$");
    }

    /**
     * Normalize and reject numbers that are not valid Kenyan mobiles.
     * Does not silently send invalid destinations.
     */
    public static String normalizeKenyanMobile(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ApiException("Phone number is required", HttpStatus.BAD_REQUEST);
        }
        String cleaned = normalize(phone);
        if (!isKenyanMobile(cleaned)) {
            throw new ApiException(
                    "Invalid phone number '" + phone.trim() + "'. Use 07…, 01…, 254…, or +254…",
                    HttpStatus.BAD_REQUEST);
        }
        return cleaned;
    }
}
