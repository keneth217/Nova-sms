package com.novastack.sms.dto.request;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationBillingModel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminCreateOrganizationRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    private OrganizationAccountType accountType = OrganizationAccountType.BUSINESS;

    private OrganizationBillingModel billingModel = OrganizationBillingModel.PREPAID;

    @Size(max = 150)
    private String adminFullName;

    @Size(min = 6, max = 100)
    private String adminPassword;

    private BigDecimal smsCost;

    /** Optional opening wallet credit (KES). */
    private BigDecimal initialCredit;
}
