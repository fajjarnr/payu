package id.payu.backoffice.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for PayU Backoffice Service.
 * Configures Swagger UI, API documentation, and security schemes.
 */
@Configuration
public class OpenApiConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayU Backoffice Service API")
                        .version("v1")
                        .description("""
                                ## PayU Backoffice Service API

                                Internal management portal for manual KYC review, fraud monitoring, and customer operations.

                                ### Authentication

                                All endpoints require authentication using a Bearer token (JWT) with backoffice admin role.
                                """)
                        .contact(new Contact()
                                .name("PayU API Support")
                                .email("api-support@payu.id")
                                .url("https://payu.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.id/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:8099").description("Local Development"),
                        new Server().url("https://staging-api.payu.id/backoffice").description("Staging"),
                        new Server().url("https://api.payu.id/backoffice").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token with backoffice admin role from Keycloak")));
    }
}
