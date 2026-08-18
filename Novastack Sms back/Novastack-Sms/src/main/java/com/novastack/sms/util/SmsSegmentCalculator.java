package com.novastack.sms.util;

import java.util.Set;

public final class SmsSegmentCalculator {

    public enum Encoding {
        GSM7,
        UCS2
    }

    public record Segmentation(Encoding encoding, int characterCount, int septetCount, int units) {
    }

    private static final int GSM7_SINGLE_LIMIT = 160;
    private static final int GSM7_CONCAT_LIMIT = 153;
    private static final int UCS2_SINGLE_LIMIT = 70;
    private static final int UCS2_CONCAT_LIMIT = 67;

    private static final String GSM7_BASIC =
            "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
                    + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà";

    private static final Set<Character> GSM7_BASIC_SET = toSet(GSM7_BASIC);
    private static final Set<Character> GSM7_EXTENDED = Set.of('|', '^', '€', '{', '}', '[', ']', '~', '\\');

    private SmsSegmentCalculator() {
    }

    public static int units(String message) {
        return analyze(message).units();
    }

    public static Segmentation analyze(String message) {
        if (message == null || message.isEmpty()) {
            return new Segmentation(Encoding.GSM7, 0, 0, 0);
        }

        if (isGsm7(message)) {
            int septets = gsm7SeptetCount(message);
            int units = septets <= GSM7_SINGLE_LIMIT
                    ? 1
                    : (int) Math.ceil(septets / (double) GSM7_CONCAT_LIMIT);
            return new Segmentation(Encoding.GSM7, message.length(), septets, units);
        }

        int chars = message.length();
        int units = chars <= UCS2_SINGLE_LIMIT
                ? 1
                : (int) Math.ceil(chars / (double) UCS2_CONCAT_LIMIT);
        return new Segmentation(Encoding.UCS2, chars, chars, units);
    }

    public static boolean isGsm7(String message) {
        if (message == null) {
            return true;
        }
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (!GSM7_BASIC_SET.contains(c) && !GSM7_EXTENDED.contains(c)) {
                return false;
            }
        }
        return true;
    }

    private static int gsm7SeptetCount(String message) {
        int count = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            count += GSM7_EXTENDED.contains(c) ? 2 : 1;
        }
        return count;
    }

    private static Set<Character> toSet(String alphabet) {
        java.util.HashSet<Character> set = new java.util.HashSet<>();
        for (int i = 0; i < alphabet.length(); i++) {
            set.add(alphabet.charAt(i));
        }
        return Set.copyOf(set);
    }
}
