package com.novastack.sms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdatePlatformSmsSettingsRequest {

    private Boolean enabled;

    @DecimalMin(value = "0.00")
    private BigDecimal lowBalanceThreshold;

    @Size(max = 255)
    private String portalUrl;

    @Size(max = 1000)
    private String welcomeTemplate;

    @Size(max = 1000)
    private String topupTemplate;

    @Size(max = 1000)
    private String collectionTemplate;

    @Size(max = 1000)
    private String lowBalanceTemplate;

    @Size(max = 160)
    private String platformTopupTemplate;

    @Size(max = 160)
    private String providerLowTemplate;

    @Size(max = 160)
    private String providerExposureTemplate;

    private List<String> collectionAccounts;

    private List<String> collectionNotifyPhones;
}
