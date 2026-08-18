package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiClientCreatedResponse {

    private ApiClientResponse client;
    private String apiKey;
}
