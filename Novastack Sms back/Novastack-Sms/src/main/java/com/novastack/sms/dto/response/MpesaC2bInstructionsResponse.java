package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpesaC2bInstructionsResponse {

    private String paybill;
    private String accountNumber;
    private String businessName;
    private String currency;
    private String instructions;
}
