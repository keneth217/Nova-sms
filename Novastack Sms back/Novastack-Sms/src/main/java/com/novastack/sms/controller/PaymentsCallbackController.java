package com.novastack.sms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.novastack.sms.mpesa.C2bCallbackResponses;
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

/**
 * C2B v2 callbacks. Paths must not contain the word "mpesa" or Safaricom rejects URL registration.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "M-Pesa Daraja Callbacks")
public class PaymentsCallbackController {

    private final WalletService walletService;

    @PostMapping(value = "/c2b/confirmation", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "C2B v2 confirmation — TransID is the M-Pesa receipt")
    public Map<String, Object> c2bConfirmation(@RequestBody JsonNode payload) {
        try {
            log.info("Received C2B v2 confirmation");
            return walletService.handleC2bConfirmation(payload);
        } catch (Exception ex) {
            log.error("C2B v2 confirmation failed; acknowledging Daraja", ex);
            return C2bCallbackResponses.accepted();
        }
    }

    @PostMapping("/c2b/validation")
    @Operation(summary = "C2B v2 validation")
    public Map<String, Object> c2bValidation(@RequestBody(required = false) JsonNode payload) {
        try {
            return walletService.handleC2bValidation(payload);
        } catch (Exception ex) {
            log.error("C2B validation failed; accepting so the payment is not cancelled", ex);
            return C2bCallbackResponses.accepted();
        }
    }

    @PostMapping(value = "/transaction-status/result", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Daraja Transaction Status result — Nova-owned C2B fallback, not a client callback")
    public Map<String, Object> transactionStatusResult(@RequestBody JsonNode payload) {
        try {
            log.info("Received Daraja Transaction Status result");
            return walletService.handleTransactionStatusResult(payload);
        } catch (Exception ex) {
            log.error("Transaction Status result failed; acknowledging Daraja", ex);
            return C2bCallbackResponses.accepted();
        }
    }

    @PostMapping(value = "/transaction-status/timeout", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.ALL_VALUE
    })
    @Operation(summary = "Daraja Transaction Status queue timeout")
    public Map<String, Object> transactionStatusTimeout(@RequestBody(required = false) JsonNode payload) {
        try {
            log.info("Received Daraja Transaction Status timeout");
            return walletService.handleTransactionStatusTimeout(payload);
        } catch (Exception ex) {
            log.error("Transaction Status timeout failed; acknowledging Daraja", ex);
            return C2bCallbackResponses.accepted();
        }
    }
}
