package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletBalanceResponse {

    private UUID walletId;
    private UUID organizationId;
    private BigDecimal balance;
    private String currency;
    private BigDecimal smsCost;
    private Long availableSms;
}
