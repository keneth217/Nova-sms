package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WalletTransactionResponse {

    private UUID id;
    private UUID organizationId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String reference;
    private String description;
    private String mpesaReceipt;
    private String phoneNumber;
    private String checkoutRequestId;
    private TopupStatus status;
    private String resultCode;
    private String resultDesc;
    private Instant createdAt;
}
