package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAnnouncementRequest {

    @NotNull
    private Boolean enabled;

    @NotBlank
    @Size(max = 40)
    private String label;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String body;

    @Size(max = 20)
    private String tone;
}
