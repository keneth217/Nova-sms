package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PaybillCollectionDashboardResponse {

    private String paybill;
    private List<String> accounts;
    private BigDecimal totalAmount;
    private long totalCount;
    private BigDecimal todayAmount;
    private long todayCount;
    private BigDecimal monthAmount;
    private long monthCount;
    private List<AccountStat> byAccount;
    private Page<PaybillCollectionResponse> recent;

    @Data
    @Builder
    public static class AccountStat {
        private String billRef;
        private long count;
        private BigDecimal amount;
    }
}
