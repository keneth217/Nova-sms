package com.novastack.sms.mpesa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpesaTransactionStatusParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void parsesCompletedC2bResult() throws Exception {
        var parsed = MpesaTransactionStatusParser.parse(MAPPER.readTree("""
                {
                  "Result": {
                    "ResultCode": 0,
                    "OriginatorConversationID": "orig-1",
                    "TransactionID": "uhja541hgh",
                    "ResultParameters": {
                      "ResultParameter": [
                        {"Key": "ReceiptNo", "Value": "UHJA541HGH"},
                        {"Key": "Amount", "Value": "100.00"},
                        {"Key": "TransactionStatus", "Value": "Completed"},
                        {"Key": "BillReferenceNumber", "Value": "NOVAC727"},
                        {"Key": "DebitPartyName", "Value": "Keneth - 254711766223"}
                      ]
                    }
                  }
                }
                """), "5687394");

        assertTrue(parsed.completed());
        assertTrue(parsed.canCredit());
        assertEquals("UHJA541HGH", parsed.receipt());
        assertEquals(0, new BigDecimal("100.00").compareTo(parsed.amount()));
        assertEquals("NOVAC727", parsed.billRef());
        assertEquals("254711766223", parsed.phone());
        assertEquals("orig-1", parsed.originatorConversationId());
    }

    @Test
    void extractsBillRefFromCreditPartyName() {
        assertEquals("NOVAC727",
                MpesaTransactionStatusParser.billRefFromCreditParty("5687394 - NOVAC727", "5687394"));
    }

    @Test
    void missingBillRefCannotCredit() throws Exception {
        var parsed = MpesaTransactionStatusParser.parse(MAPPER.readTree("""
                {
                  "Result": {
                    "ResultCode": 0,
                    "TransactionID": "UHJA541HGH",
                    "ResultParameters": {
                      "ResultParameter": [
                        {"Key": "Amount", "Value": "50"},
                        {"Key": "TransactionStatus", "Value": "Completed"}
                      ]
                    }
                  }
                }
                """), "5687394");

        assertTrue(parsed.completed());
        assertFalse(parsed.canCredit());
    }

    @Test
    void nonZeroResultCodeIsNotCompleted() {
        assertFalse(MpesaTransactionStatusParser.isCompleted("2001", "Completed"));
        assertTrue(MpesaTransactionStatusParser.isCompleted("0", "Completed"));
        assertTrue(MpesaTransactionStatusParser.isCompleted("0", null));
    }
}
