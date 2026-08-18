package com.novastack.sms.util;

import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void normalizesKenyanMobilesFromCommonFormats() {
        assertEquals("254712345678", PhoneNormalizer.normalizeKenyanMobile("0712345678"));
        assertEquals("254712345678", PhoneNormalizer.normalizeKenyanMobile("+254712345678"));
        assertEquals("254712345678", PhoneNormalizer.normalizeKenyanMobile("254712345678"));
        assertEquals("254712345678", PhoneNormalizer.normalizeKenyanMobile("0712 345 678"));
        assertEquals("254112345678", PhoneNormalizer.normalizeKenyanMobile("0112345678"));
    }

    @Test
    void rejectsInvalidKenyanNumbers() {
        ApiException ex = assertThrows(ApiException.class, () -> PhoneNormalizer.normalizeKenyanMobile("12345"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid phone number"));

        assertThrows(ApiException.class, () -> PhoneNormalizer.normalizeKenyanMobile(""));
        assertThrows(ApiException.class, () -> PhoneNormalizer.normalizeKenyanMobile("abcdefghij"));
        assertFalse(PhoneNormalizer.isKenyanMobile("25471234567"));
    }
}
