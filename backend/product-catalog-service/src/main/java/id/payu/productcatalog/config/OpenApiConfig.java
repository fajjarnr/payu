package id.payu.productcatalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productCatalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product Catalog Service API")
                        .description("API for managing product definitions and configurations")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PayU Engineering")
                                .email("engineering@payu.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.id/license")));
    }
}
