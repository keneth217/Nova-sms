package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationBillingModel;
import com.novastack.sms.domain.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AdminOrganizationResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String mpesaAccountRef;
    private OrganizationStatus status;
    private OrganizationAccountType accountType;
    private OrganizationBillingModel billingModel;
    private Instant expiresAt;
    private BigDecimal smsCost;
    private BigDecimal walletBalance;
    private String currency;
    private long userCount;
    private Instant createdAt;
}
