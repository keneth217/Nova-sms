package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TalkSasaAccountResponse {

    private boolean configured;
    private boolean reachable;
    private String errorMessage;
    private Profile profile;
    private Balance balance;

    @Data
    @Builder
    public static class Profile {
        private String name;
        private String email;
        private String phone;
        private String country;
        private String timezone;
        private String status;
    }

    @Data
    @Builder
    public static class Balance {
        private BigDecimal remainingUnits;
        private BigDecimal totalUnits;
        private BigDecimal usedUnits;
        private String unitType;
        private String expiredOn;
    }
}
