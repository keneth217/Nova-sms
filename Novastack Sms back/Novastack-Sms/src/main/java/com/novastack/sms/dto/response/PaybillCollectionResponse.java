package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaybillCollectionResponse {

    private UUID id;
    private String billRef;
    private BigDecimal amount;
    private String mpesaReceipt;
    private String phoneNumber;
    private String mpesaTransactionDate;
    private String payerName;
    private Instant createdAt;
}
