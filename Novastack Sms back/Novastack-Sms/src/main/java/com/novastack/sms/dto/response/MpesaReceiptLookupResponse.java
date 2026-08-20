package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.TopupStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MpesaReceiptLookupResponse {

    private String mpesaReceipt;
    private boolean found;
    /** WALLET, COLLECTION, or NONE */
    private String source;
    private boolean walletCredited;
    private boolean needsManualRecovery;
    private boolean recoverableFromCallback;
    private UUID transactionId;
    private UUID organizationId;
    private String organizationName;
    private BigDecimal amount;
    private TopupStatus status;
    private String billRef;
    private String message;
}
