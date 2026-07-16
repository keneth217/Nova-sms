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
    private List<SmsMessageResponse> messages;
}
