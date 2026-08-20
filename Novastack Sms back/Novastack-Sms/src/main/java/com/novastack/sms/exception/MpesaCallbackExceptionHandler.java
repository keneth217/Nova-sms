package com.novastack.sms.exception;

import com.novastack.sms.controller.MpesaCallbackController;
import com.novastack.sms.controller.PaymentsCallbackController;
import com.novastack.sms.mpesa.C2bCallbackResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Safaricom retries and spike-arrests if callback URLs return HTTP 4xx/5xx.
 * Always ACK with HTTP 200 and ResultCode 0 when the handler itself blows up.
 */
@RestControllerAdvice(assignableTypes = {PaymentsCallbackController.class, MpesaCallbackController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MpesaCallbackExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        log.warn("M-Pesa callback body was empty or not JSON: {}",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        return ResponseEntity.ok(C2bCallbackResponses.accepted());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> any(Exception ex) {
        log.error("M-Pesa callback failed; acknowledging so Daraja does not spike-arrest", ex);
        return ResponseEntity.ok(C2bCallbackResponses.accepted());
    }
}
