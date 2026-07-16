package com.novastack.sms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novastackOpenApi() {
        final String bearer = "bearerAuth";
        final String apiKey = "apiKeyAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Novastack SMS Gateway API")
                        .description("Multi-tenant Bulk SMS Gateway with wallet and Africa's Talking")
                        .version("1.0.0")
                        .contact(new Contact().name("Novastack").email("support@novastack.com")))
                .addSecurityItem(new SecurityRequirement().addList(bearer).addList(apiKey))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .name(bearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(apiKey, new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
