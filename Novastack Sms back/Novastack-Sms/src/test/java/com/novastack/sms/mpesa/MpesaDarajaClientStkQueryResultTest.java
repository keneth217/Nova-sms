package com.novastack.sms.mpesa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpesaDarajaClientStkQueryResultTest {

    @Test
    void resultCodeZeroIsSuccess() {
        MpesaDarajaClient.StkQueryResult result = query("0", "The service request is processed successfully.", "0", null);
        assertTrue(result.isPaymentSuccessful());
        assertFalse(result.isStillProcessing());
        assertFalse(result.isTerminalFailure());
    }

    @Test
    void stillUnderProcessingIsPendingNotFailed() {
        MpesaDarajaClient.StkQueryResult result = query(
                "500.001.1001",
                "The transaction is still under processing",
                "500.001.1001",
                "The transaction is still under processing");
        assertFalse(result.isPaymentSuccessful());
        assertTrue(result.isStillProcessing());
        assertTrue(result.hasProcessingDescriptor());
        assertFalse(result.isTerminalFailure());
    }

    @Test
    void beingProcessedIsPending() {
        MpesaDarajaClient.StkQueryResult result = query("0", "The transaction is being processed", "4999", "Request processing");
        assertTrue(result.isStillProcessing());
        assertFalse(result.isTerminalFailure());
    }

    @Test
    void missingResultCodeIsPending() {
        MpesaDarajaClient.StkQueryResult result = query("0", "The service request has been accepted successfully", null, null);
        assertTrue(result.isStillProcessing());
        assertFalse(result.isTerminalFailure());
    }

    @Test
    void userCancelledIsTerminalFailure() {
        MpesaDarajaClient.StkQueryResult result = query("0", "accepted", "1032", "Request cancelled by user");
        assertTrue(result.isTerminalFailure());
        assertFalse(result.isStillProcessing());
    }

    @Test
    void insufficientFundsIsTerminalFailure() {
        MpesaDarajaClient.StkQueryResult result = query("0", "accepted", "1", "The balance is insufficient for the transaction");
        assertTrue(result.isTerminalFailure());
    }

    @Test
    void wrongPinIsTerminalFailure() {
        MpesaDarajaClient.StkQueryResult result = query("0", "accepted", "2001", "The initiator information is invalid");
        assertTrue(result.isTerminalFailure());
    }

    private static MpesaDarajaClient.StkQueryResult query(
            String responseCode,
            String responseDescription,
            String resultCode,
            String resultDesc) {
        return new MpesaDarajaClient.StkQueryResult(
                responseCode,
                responseDescription,
                "m-1",
                "ws_CO_123",
                resultCode,
                resultDesc,
                "{}");
    }
}
