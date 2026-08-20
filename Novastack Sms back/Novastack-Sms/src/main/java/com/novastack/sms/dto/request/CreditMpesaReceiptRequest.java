package com.novastack.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditMpesaReceiptRequest {

    @NotBlank
    @Size(min = 8, max = 20)
    private String mpesaReceipt;

    /** Required only when Nova has no stored C2B callback for this receipt. */
    @Size(min = 3, max = 32)
    private String accountNumber;

    /** Required only when Nova has no stored C2B callback for this receipt. */
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
