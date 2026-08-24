package com.novastack.sms.controller;

import com.novastack.sms.dto.request.WalletTopupRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MpesaControllerTest {

    @Test
    void stkRequestHashIgnoresAmountScale() {
        WalletTopupRequest scaled = request("254712345678", new BigDecimal("500.00"));
        WalletTopupRequest plain = request("254712345678", new BigDecimal("500"));
        assertEquals(MpesaController.stkRequestHash(scaled), MpesaController.stkRequestHash(plain));
    }

    @Test
    void stkRequestHashChangesWhenPhoneOrAmountChanges() {
        WalletTopupRequest base = request("254712345678", new BigDecimal("500"));
        assertNotEquals(MpesaController.stkRequestHash(base),
                MpesaController.stkRequestHash(request("254700000000", new BigDecimal("500"))));
        assertNotEquals(MpesaController.stkRequestHash(base),
                MpesaController.stkRequestHash(request("254712345678", new BigDecimal("100"))));
    }

    private static WalletTopupRequest request(String phone, BigDecimal amount) {
        WalletTopupRequest request = new WalletTopupRequest();
        request.setPhoneNumber(phone);
        request.setAmount(amount);
        return request;
    }
}
