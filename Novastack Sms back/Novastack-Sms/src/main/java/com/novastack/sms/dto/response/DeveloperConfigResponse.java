package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeveloperConfigResponse {

    private String publicBaseUrl;
    private String apiBaseUrl;

    private String openApiPath;
    private String swaggerUiPath;
}
