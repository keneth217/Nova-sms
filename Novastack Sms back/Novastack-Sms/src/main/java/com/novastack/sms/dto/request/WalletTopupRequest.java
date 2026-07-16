package com.novastack.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTopupRequest {

    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum top-up is 1 KES")
    private BigDecimal amount;

    /** M-Pesa phone that will receive the STK Push, e.g. 2547XXXXXXXX */
    @NotBlank
    @Size(min = 9, max = 15)
    private String phoneNumber;
}
