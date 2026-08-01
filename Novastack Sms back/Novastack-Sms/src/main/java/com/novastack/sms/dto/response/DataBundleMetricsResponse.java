package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DataBundleMetricsResponse {
    private long totalSold;
    private long successful;
    private long failed;
    private long pending;
    private BigDecimal revenue;
}
