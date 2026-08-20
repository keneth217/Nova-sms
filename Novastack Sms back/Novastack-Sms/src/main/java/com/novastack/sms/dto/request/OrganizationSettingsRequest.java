package com.novastack.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrganizationSettingsRequest {

    @Size(max = 150)
    private String name;

    @Email
    @Size(max = 180)
    private String email;

    @Size(max = 30)
    private String phone;

    @NotNull
    private Boolean notificationsEnabled;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true, message = "Low-balance threshold must be 0 or greater")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal lowBalanceThreshold;
}
