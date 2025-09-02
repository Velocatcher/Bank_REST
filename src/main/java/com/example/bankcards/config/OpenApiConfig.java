package com.example.bankcards.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
// чтобы не писать @SecurityRequirement на каждом методе:
@OpenAPIDefinition(security = { @SecurityRequirement(name = "bearerAuth") })

public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        // почему: читаемая шапка в Swagger UI и машиночитаемая мета информация
        return new OpenAPI().info(new Info()
                .title("Bank Cards API")
                .version("1.0.0")
                .description("Simple cards management with JWT and roles"));
    }
}
