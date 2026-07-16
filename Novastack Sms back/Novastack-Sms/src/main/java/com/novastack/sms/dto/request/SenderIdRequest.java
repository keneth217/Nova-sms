package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SenderIdRequest {

    @NotBlank
    @Size(min = 3, max = 11)
    private String senderName;
}
