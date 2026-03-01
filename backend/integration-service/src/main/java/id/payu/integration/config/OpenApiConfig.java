package id.payu.integration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Integration Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${payu.integration.api.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI integrationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayU Integration Service API")
                        .description("Legacy system integration layer for SWIFT, OJK reporting, and SOAP")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PayU Engineering")
                                .email("engineering@payu.fajjjar.my.id")
                                .url("https://payu.fajjjar.my.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.fajjjar.my.id/license")))
                .servers(List.of(
                        new Server().url(serverUrl).description("Integration Service Server"),
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}
