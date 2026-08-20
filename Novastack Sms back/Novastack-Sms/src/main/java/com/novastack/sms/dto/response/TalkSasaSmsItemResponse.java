package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TalkSasaSmsItemResponse {

    private String uid;
    private String recipient;
    private String senderId;
    private String message;
    private String status;
    private String type;
    private String direction;
    private String cost;
    private Integer smsCount;
    private String createdAt;
    private SmsMessageResponse novaMessage;
}
