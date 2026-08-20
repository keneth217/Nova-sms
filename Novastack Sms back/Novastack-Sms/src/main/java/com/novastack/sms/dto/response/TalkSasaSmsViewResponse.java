package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TalkSasaSmsViewResponse {

    private boolean configured;
    private boolean reachable;
    private String errorMessage;
    private TalkSasaSmsItemResponse item;
}
