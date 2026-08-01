package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DataBundleOffersRequest {

    /**
     * Safaricom MSISDN: 07… / 011… / 2547… / 25411…
     */
    @NotBlank
    @Pattern(
            regexp = "^(?:254|0)?(?:7\\d{8}|11\\d{7})$",
            message = "Enter a valid Safaricom number (07…, 011…, 2547…, or 25411…)")
    private String phoneNumber;
}
