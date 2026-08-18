package com.novastack.sms.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsSegmentCalculatorTest {

    @Test
    void shortGsmMessageIsOneUnit() {
        assertEquals(1, SmsSegmentCalculator.units("Hello Nova SMS"));
        assertEquals(SmsSegmentCalculator.Encoding.GSM7, SmsSegmentCalculator.analyze("Hello").encoding());
    }

    @Test
    void oneHundredSixtyGsmCharactersIsOneUnit() {
        String message = "a".repeat(160);
        assertEquals(1, SmsSegmentCalculator.units(message));
    }

    @Test
    void oneHundredSixtyOneGsmCharactersIsTwoUnits() {
        String message = "a".repeat(161);
        assertEquals(2, SmsSegmentCalculator.units(message));
    }

    @Test
    void unicodeMessageUsesUcs2() {
        String message = "Habari 😊";
        var result = SmsSegmentCalculator.analyze(message);
        assertEquals(SmsSegmentCalculator.Encoding.UCS2, result.encoding());
        assertEquals(1, result.units());
        assertTrue(message.length() <= 70);
    }

    @Test
    void longUnicodeMessageIsMultipart() {
        String message = "你好".repeat(40); // 80 UCS-2 chars
        var result = SmsSegmentCalculator.analyze(message);
        assertEquals(SmsSegmentCalculator.Encoding.UCS2, result.encoding());
        assertEquals(2, result.units());
    }

    @Test
    void multipartGsmUsesOneHundredFiftyThreePerPart() {
        String message = "a".repeat(306);
        assertEquals(2, SmsSegmentCalculator.units(message));
        assertEquals(3, SmsSegmentCalculator.units("a".repeat(307)));
    }
}
