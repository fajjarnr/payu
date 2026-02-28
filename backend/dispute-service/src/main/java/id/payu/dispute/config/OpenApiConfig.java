package id.payu.dispute.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for Dispute Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayU Dispute Service API")
                        .version("1.0.0")
                        .description("API for managing refunds and disputes in the PayU Digital Banking Platform")
                        .contact(new Contact()
                                .name("PayU Support")
                                .email("support@payu.fajjjar.my.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.fajjjar.my.id/license")));
    }
}
