package com.novastack.sms.provider;

import com.novastack.sms.domain.enums.MessageStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TalkSasaStatusMapperTest {

    @Test
    void mapsKnownTalkSasaStatuses() {
        assertEquals(MessageStatus.ACCEPTED, TalkSasaStatusMapper.toInternal("accepted"));
        assertEquals(MessageStatus.ACCEPTED, TalkSasaStatusMapper.toInternal("  ACCEPTED  "));
        assertEquals(MessageStatus.PROCESSING, TalkSasaStatusMapper.toInternal("processing"));
        assertEquals(MessageStatus.SENT, TalkSasaStatusMapper.toInternal("Sent"));
        assertEquals(MessageStatus.DELIVERED, TalkSasaStatusMapper.toInternal("delivered"));
        assertEquals(MessageStatus.DELIVERED, TalkSasaStatusMapper.toInternal("completed"));
        assertEquals(MessageStatus.FAILED, TalkSasaStatusMapper.toInternal("failed"));
        assertEquals(MessageStatus.REJECTED, TalkSasaStatusMapper.toInternal("rejected"));
        assertEquals(MessageStatus.CANCELLED, TalkSasaStatusMapper.toInternal("cancelled"));
    }

    @Test
    void completedQueueWithNoFailuresIsDelivered() {
        assertEquals(
                MessageStatus.DELIVERED,
                TalkSasaStatusMapper.fromQueue("completed", 0, 0, 1, 1, null));
    }

    @Test
    void completedQueueWithSingleRecipientFailureIsFailed() {
        assertEquals(
                MessageStatus.FAILED,
                TalkSasaStatusMapper.fromQueue("completed", 1, 0, 1, 1, null));
    }

    @Test
    void completedMixedBulkDoesNotMarkDelivered() {
        assertNull(TalkSasaStatusMapper.fromQueue("completed", 1, 0, 3, 3, null));
    }
}
