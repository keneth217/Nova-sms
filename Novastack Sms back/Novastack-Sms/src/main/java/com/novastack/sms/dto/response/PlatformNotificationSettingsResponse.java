package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlatformNotificationSettingsResponse {

    private boolean enabled;
    private BigDecimal lowBalanceThreshold;
    private String portalUrl;
    private String welcomeTemplate;
    private String topupTemplate;
    private String collectionTemplate;
    private String lowBalanceTemplate;
    private String platformTopupTemplate;
    private String providerLowTemplate;
    private String providerExposureTemplate;
    private BigDecimal talksasaLastRemaining;
    private boolean talksasaLowAlerted;
    private boolean talksasaExposureAlerted;
    private List<String> collectionAccounts;
    private List<String> collectionNotifyPhones;
}
