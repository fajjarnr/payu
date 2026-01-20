package id.payu.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayU Transaction Service API")
                        .description("Transaction processing, BI-FAST, QRIS payments for PayU Digital Banking Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PayU Backend Team")
                                .email("backend-team@payu.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.id")))
                .addServersItem(new Server()
                                .url("http://localhost:8003")
                                .description("Development Server"))
                .addServersItem(new Server()
                                .url("/v1")
                                .description("Production API Path"));
    }
}
