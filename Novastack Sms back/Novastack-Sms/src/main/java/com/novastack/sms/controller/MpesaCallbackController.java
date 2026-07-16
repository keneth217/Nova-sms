package com.novastack.sms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.novastack.sms.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "M-Pesa Daraja Callbacks")
public class MpesaCallbackController {

    private final WalletService walletService;

    /**
     * Safaricom posts here after the customer completes (or cancels) STK Push.
     * Must be publicly reachable HTTPS — set novastack.mpesa.callback-base-url.
     */
    @PostMapping(value = "/stk/callback", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Safaricom STK Push result callback — updates wallet_transactions and credits wallet")
    public Map<String, Object> stkCallback(@RequestBody JsonNode payload) {
        log.info("Received Safaricom STK callback");
        walletService.handleStkCallback(payload);
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }

    @PostMapping(value = "/c2b/confirmation", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Daraja C2B confirmation when customer pays Paybill manually")
    public Map<String, Object> c2bConfirmation(@RequestBody Map<String, String> payload) {
        log.info("Received C2B confirmation");
        return walletService.handleC2bConfirmation(payload);
    }

    @PostMapping("/c2b/validation")
    @Operation(summary = "Daraja C2B validation")
    public Map<String, Object> c2bValidation(@RequestBody(required = false) Map<String, String> payload) {
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }
}
