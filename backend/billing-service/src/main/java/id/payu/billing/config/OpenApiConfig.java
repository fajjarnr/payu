package id.payu.billing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Billing Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Bean
    public OpenAPI billingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayU Billing Service API")
                        .description("Bill payment API for PLN, PDAM, Pulsa, E-wallet top-up, and other billers")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PayU API Team")
                                .email("api@payu.fajjjar.my.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.fajjjar.my.id")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Local Development"),
                        new Server().url("https://billing-service.payu.fajjjar.my.id").description("Production")
                ))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                        .addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication")));
    }
}
