package com.novastack.sms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNormalizerTest {

    @Test
    void normalizesLegacy07AndNew011SafaricomPrefixes() {
        assertEquals("254712345678", PhoneNormalizer.normalize("0712345678"));
        assertEquals("254712345678", PhoneNormalizer.normalize("254712345678"));
        assertEquals("254117979906", PhoneNormalizer.normalize("0117979906"));
        assertEquals("254117979906", PhoneNormalizer.normalize("254117979906"));
        assertEquals("254117979906", PhoneNormalizer.normalize("117979906"));
    }

    @Test
    void acceptsSafaricomInternationalForms() {
        assertTrue(PhoneNormalizer.isSafaricomMsisdn(PhoneNormalizer.normalize("0117979906")));
        assertTrue(PhoneNormalizer.isSafaricomMsisdn(PhoneNormalizer.normalize("0712345678")));
    }
}
