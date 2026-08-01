package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DataBundlePurchaseRequest {

    @NotBlank
    @Pattern(
            regexp = "^(?:254|0)?(?:7\\d{8}|11\\d{7})$",
            message = "Enter a valid Safaricom number (07…, 011…, 2547…, or 25411…)")
    private String phoneNumber;

    @NotBlank
    @Size(max = 64)
    private String offerId;

    /** Optional fingerprint from fetch — used to re-resolve live offeringId if CVM ids rotated. */
    @Size(max = 64)
    private String accountId;

    private java.math.BigDecimal amount;

    @Size(max = 32)
    private String resourceAmount;

    @Size(max = 40)
    private String reference;

    /** airtime or m-pesa (Safaricom paymentMode). */
    @Pattern(regexp = "(?i)^(airtime|m-?pesa|mpesa)?$", message = "Payment mode must be airtime or m-pesa")
    private String paymentMode = "airtime";

    /**
     * Optional M-Pesa payer MSISDN when different from {@link #phoneNumber} (bundle recipient).
     */
    @Pattern(
            regexp = "^$|^(?:254|0)?(?:7\\d{8}|11\\d{7})$",
            message = "Enter a valid M-Pesa payment number (07…, 011…, 2547…, or 25411…)")
    private String paymentPhoneNumber;
}
