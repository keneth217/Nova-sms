package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DataBundleOfferResponse {
    /** Safaricom {@code offeringId} — required for purchase. */
    private String offerId;
    /** Safaricom {@code uniqueOfferingId} (catalog id; not used as purchase offeringId). */
    private String uniqueOfferingId;
    private String offerName;
    private String category;
    private BigDecimal amount;
    private String validity;
    private String description;
    /** Safaricom resourceAccId required for purchase. */
    private String accountId;
    /** Resource awarded in MB (resourceValue). */
    private String resourceAmount;
    private String offerSource;
    private String parentOfferId;
}
