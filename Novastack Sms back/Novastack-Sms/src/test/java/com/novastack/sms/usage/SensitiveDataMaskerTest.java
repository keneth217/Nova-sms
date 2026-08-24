package com.novastack.sms.usage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataMaskerTest {

    @Test
    void masksKenyanMsisdn() {
        String masked = SensitiveDataMasker.maskMsisdn("254711766223");
        assertTrue(masked.startsWith("254711"));
        assertTrue(masked.contains("****"));
        assertTrue(masked.endsWith("23"));
        assertEquals("254711****23", masked);
    }

    @Test
    void masksEmbeddedNumbersInText() {
        String masked = SensitiveDataMasker.maskText("payer 254711766223 done");
        assertTrue(masked.contains("****"));
        assertTrue(!masked.contains("254711766223"));
    }

    @Test
    void takesFirstForwardedIp() {
        assertEquals("10.0.0.8", SensitiveDataMasker.clientIp("10.0.0.8, 10.1.1.1", "127.0.0.1"));
    }
}
