package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.TopupStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StkPushResponse {

    private UUID transactionId;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String customerMessage;
    private TopupStatus status;
    private BigDecimal amount;
    private String phoneNumber;
    private String mpesaReceipt;
    private String resultCode;
    private String resultDesc;
    /** True when Safaricom STK callback has been applied to this transaction. */
    private boolean callbackReceived;
    private boolean walletCredited;
    private Instant updatedAt;
}
