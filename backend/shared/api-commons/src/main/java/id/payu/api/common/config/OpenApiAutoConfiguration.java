package id.payu.api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

    @Value("${spring.application.name:payu-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    private final OpenApiProperties properties;

    public OpenApiAutoConfiguration(OpenApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenAPI() {
        String title = properties.getTitle() != null ? properties.getTitle() : formatTitle(applicationName);
        String description = properties.getDescription() != null ? properties.getDescription() : "API for " + title;

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(properties.getVersion())
                        .contact(new Contact()
                                .name(properties.getContactName())
                                .email(properties.getContactEmail()))
                        .license(new License()
                                .name(properties.getLicenseName())
                                .url(properties.getLicenseUrl())));

        // Configure servers
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("http://localhost:" + serverPort).description("Local Development"));
        
        String cleanAppName = applicationName.replace("-service", "");
        servers.add(new Server().url("https://" + cleanAppName + "-service.payu.fajjjar.my.id").description("Production"));
        openAPI.servers(servers);

        if (properties.isAddBearerAuth()) {
            openAPI.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                    .components(new io.swagger.v3.oas.models.Components()
                            .addSecuritySchemes("bearerAuth",
                                    new SecurityScheme()
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                                            .description("JWT token for authentication")));
        }

        return openAPI;
    }

    private String formatTitle(String appName) {
        if (appName == null || appName.isEmpty()) {
            return "PayU Service API";
        }
        String[] words = appName.split("-");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim() + " API";
    }
}
