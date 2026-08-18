package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeveloperPortalServiceTest {

    @Test
    void publicConfigUsesConfiguredOrigin() {
        AppProperties properties = new AppProperties();
        properties.getApi().setPublicBaseUrl("https://smsapi.novastack.co.ke/");
        DeveloperPortalService service = new DeveloperPortalService(
                properties, null, null, null, null, null);

        var config = service.publicConfig();
        assertEquals("https://smsapi.novastack.co.ke", config.getPublicBaseUrl());
        assertEquals("https://smsapi.novastack.co.ke/api/v1", config.getApiBaseUrl());
        assertEquals("/v3/api-docs", config.getOpenApiPath());
        assertEquals("/swagger-ui.html", config.getSwaggerUiPath());
    }
}
