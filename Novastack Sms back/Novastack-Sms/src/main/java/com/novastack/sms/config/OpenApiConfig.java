package com.novastack.sms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novastackOpenApi(AppProperties appProperties) {
        final String bearer = "bearerAuth";
        final String apiKey = "apiKeyAuth";
        String publicBase = trimSlash(appProperties.getApi().getPublicBaseUrl());

        return new OpenAPI()
                .info(new Info()
                        .title("Nova SMS API")
                        .description("""
                                Multi-tenant SMS SaaS and developer gateway. External applications authenticate \
                                with `X-API-Key` and call `/api/v1/sms/**`, `/api/v1/wallet/**` when granted \
                                `WALLET_READ` / `WALLET_TOPUP`, `/api/v1/mpesa/stkpush` or `/checkout` when granted \
                                `MPESA_STK_PUSH` (or `WALLET_TOPUP`), and `/api/v1/mpesa/c2b/transactions` when granted \
                                `MPESA_C2B` (or `WALLET_READ` / `WALLET_TOPUP`). Clients do not configure or \
                                implement Safaricom callbacks — Nova handles STK and C2B internally; apps \
                                retrieve status through the authenticated API. Never send TalkSasa or Daraja \
                                tokens. Optional `Idempotency-Key` on POST /sms/send, POST /sms/bulk, \
                                POST /sms/batches/{id}/resend-failed, POST /mpesa/stkpush, POST /mpesa/checkout, \
                                and POST /wallet/topup prevents duplicate work.""")
                        .version("1.0.0")
                        .contact(new Contact().name("Novastack").email("support@novastack.com")))
                .addServersItem(new Server().url(publicBase).description("Configured public API origin"))
                .addServersItem(new Server().url("http://localhost:8092").description("Local development"))
                .addSecurityItem(new SecurityRequirement().addList(bearer).addList(apiKey))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .name(bearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dashboard users. Not used by API clients."))
                        .addSecuritySchemes(apiKey, new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Hashed Nova SMS live key (`nova_live_…`). Shown once at create/rotate.")));
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://smsapi.novastack.co.ke";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url.trim();
    }
}
