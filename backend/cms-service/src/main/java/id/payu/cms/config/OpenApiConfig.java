package id.payu.cms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for CMS Service
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "keycloak";

        return new OpenAPI()
            .info(new Info()
                .title("PayU CMS Service API")
                .description("Content Management Service for banners, promotions, alerts, and popups. " +
                            "Supports scheduled publishing, targeting rules, and A/B test integration.")
                .version(applicationVersion)
                .license(new License()
                    .name("Proprietary")
                    .url("https://payu.id")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8082")
                    .description("Local development"),
                new Server()
                    .url("https://cms-service.payu.svc.cluster.local:8080")
                    .description("OpenShift internal cluster"),
                new Server()
                    .url("https://api.payu.id/cms")
                    .description("Production API Gateway")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token from Keycloak authentication")));
    }
}
