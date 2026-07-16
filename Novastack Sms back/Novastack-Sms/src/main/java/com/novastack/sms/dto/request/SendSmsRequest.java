package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendSmsRequest {

    @NotBlank
    @Size(max = 20)
    private String recipient;

    @NotBlank
    @Size(max = 1600)
    private String message;

    @Size(max = 11)
    private String senderId;
}
