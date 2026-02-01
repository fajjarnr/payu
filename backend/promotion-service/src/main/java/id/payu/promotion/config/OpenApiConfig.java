package id.payu.promotion.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PayU Promotion Service API",
        version = "1.0.0",
        description = "APIs for managing promotions, cashbacks, rewards, referrals, and loyalty points in the PayU platform",
        contact = @Contact(
            name = "PayU Platform Team",
            email = "platform@payu.id"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://payu.id/license"
        )
    ),
    servers = {
        @Server(
            description = "Local Development",
            url = "http://localhost:8080"
        ),
        @Server(
            description = "Development Environment",
            url = "https://promotion-dev.payu.id"
        ),
        @Server(
            description = "Production Environment",
            url = "https://production.payu.id"
        )
    },
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT authorization token using Bearer scheme")));
    }
}
