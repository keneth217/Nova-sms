package com.novastack.sms.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProviderErrorMessagesTest {

    @Test
    void mapsInvalidSenderFromVendorDetail() {
        assertEquals(
                "The selected sender id is invalid.",
                ProviderErrorMessages.fromVendorDetail("The selected sender id is invalid.", 422));
        assertEquals(
                "Sender ID TALK_SASA is not authorized to send this message.",
                ProviderErrorMessages.fromVendorDetail(
                        "Originator TALK_SASA is not authorized to send this message", 404));
    }

    @Test
    void mapsInvalidRecipientFromVendorDetail() {
        assertEquals(
                "Recipient phone is invalid.",
                ProviderErrorMessages.fromVendorDetail("recipient phone is invalid", 422));
    }

    @Test
    void mapsMissingUnitsFromVendorDetail() {
        assertEquals(
                ProviderErrorMessages.PROVIDER_UNITS,
                ProviderErrorMessages.fromVendorDetail("Insufficient units", 422));
    }

    @Test
    void fallsBackToHttpStatusMapping() {
        assertEquals(ProviderErrorMessages.AUTH, ProviderErrorMessages.fromVendorDetail(null, 401));
        assertEquals(ProviderErrorMessages.VALIDATION, ProviderErrorMessages.fromVendorDetail("nope", 422));
    }
}
