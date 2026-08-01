package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.BundleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DataBundleTransactionResponse {
    private UUID id;
    private String reference;
    private String phoneNumber;
    private String offerId;
    private String offerName;
    private String category;
    private BigDecimal amount;
    private BundleStatus status;
    private String checkoutRequestId;
    private String responseCode;
    private String responseDescription;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
