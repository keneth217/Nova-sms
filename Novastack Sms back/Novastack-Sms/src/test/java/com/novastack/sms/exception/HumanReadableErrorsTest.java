package com.novastack.sms.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HumanReadableErrorsTest {

    @Test
    void humanizesOriginatorVendorMessage() {
        assertEquals(
                "Sender ID TALK_SASA is not authorized to send this message.",
                HumanReadableErrors.fromVendor("Originator TALK_SASA is not authorized to send this message"));
    }

    @Test
    void humanizesOriginatorFromJsonBody() {
        assertEquals(
                "Sender ID TALK_SASA is not authorized to send this message.",
                HumanReadableErrors.fromVendor(
                        "{\"status\":\"error\",\"message\":\"Originator TALK_SASA is not authorized to send this message\"}"));
    }

    @Test
    void hidesSqlAndStackTraces() {
        assertNull(HumanReadableErrors.fromVendor("Duplicate entry 'a@b.com' for key 'users.uk_users_email'"));
        assertNull(HumanReadableErrors.fromVendor("could not execute statement [Duplicate entry]"));
        assertEquals(HumanReadableErrors.GENERIC, HumanReadableErrors.fromException(
                new IllegalStateException("could not execute statement")));
    }

    @Test
    void prefersApiExceptionMessage() {
        assertEquals(
                "Email already registered",
                HumanReadableErrors.fromException(new ApiException("Email already registered", org.springframework.http.HttpStatus.CONFLICT)));
    }
}
