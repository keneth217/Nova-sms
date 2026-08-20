package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyMpesaReceiptRequest {

    @NotBlank
    @Size(min = 8, max = 20)
    private String mpesaReceipt;
}
