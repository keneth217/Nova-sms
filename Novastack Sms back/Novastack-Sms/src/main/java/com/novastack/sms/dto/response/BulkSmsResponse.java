package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkSmsResponse {

    private UUID batchId;
    private int queuedCount;
    private int recipientCount;
    private int smsUnits;
    private String status;
    private UUID sourceBatchId;
    private Integer resentCount;
    private Integer skippedCount;
    private Integer failedCount;
    private List<SmsMessageResponse> messages;
}
