package com.novastack.sms.dto.response;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String apiKey;
    private String mpesaAccountRef;
    private OrganizationStatus status;
    private OrganizationAccountType accountType;
    private Instant expiresAt;
    private Integer activeDays;
    private Instant createdAt;
    /** Prepaid wallet created at registration for top-ups and SMS sending. */
    private UUID walletId;
    private BigDecimal walletBalance;
    private String walletCurrency;
}
