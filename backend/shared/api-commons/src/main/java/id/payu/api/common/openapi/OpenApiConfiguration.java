package id.payu.api.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for PayU Digital Banking Platform.
 * Configures Swagger UI, API documentation, and security schemes.
 */
@Configuration
public class OpenApiConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";
    public static final String API_TITLE = "PayU Digital Banking API";
    public static final String API_VERSION = "v1";
    public static final String API_DESCRIPTION = """
            ## PayU Digital Banking Platform API

            Welcome to the PayU Digital Banking API documentation.

            ### Authentication

            Most endpoints require authentication using a Bearer token (JWT).

            ```http
            Authorization: Bearer <your-token>
            ```

            ### Request Headers

            | Header | Required | Description |
            |--------|----------|-------------|
            | `X-Request-ID` | No | Unique request identifier for tracing |
            | `X-Correlation-ID` | No | Correlation ID for distributed tracing |
            | `X-Device-ID` | No | Device identifier |
            | `Accept-Language` | No | Localization (default: id-ID) |

            ### Error Responses

            All errors follow a consistent format:

            ```json
            {
              "success": false,
              "error": {
                "code": "ERROR_CODE",
                "message": "Human-readable error message"
              },
              "meta": {
                "requestId": "req-abc-123",
                "timestamp": "2026-01-28T10:30:00Z"
              }
            }
            ```

            ### Rate Limiting

            - Default: 100 requests per minute
            - Strict (sensitive operations): 10 requests per minute

            ### Pagination

            List endpoints support pagination:

            ```
            GET /v1/resource?page=0&size=20&sort=createdAt,desc
            ```

            - `page`: Page number (0-based, default: 0)
            - `size`: Items per page (default: 20, max: 100)
            - `sort`: Sort field and direction (default: createdAt,desc)
            """;

    @Bean
    public OpenAPI payUOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .version(API_VERSION)
                        .description(API_DESCRIPTION)
                        .contact(new Contact()
                                .name("PayU API Support")
                                .email("api-support@payu.id")
                                .url("https://payu.id"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://payu.id/terms")))
                .servers(List.of(
                        new Server().url("https://api.payu.id/v1").description("Production"),
                        new Server().url("https://api.staging.payu.id/v1").description("Staging"),
                        new Server().url("http://localhost:8080/api/v1").description("Local Development")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from Keycloak authentication"))
                        .addSchemas("Money", moneySchema())
                        .addSchemas("Page", pageSchema())
                        .addSchemas("Error", errorSchema())
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * Customizer to add common parameters to all operations.
     */
    @Bean
    public OpenApiCustomizer payUOpenApiCustomizer() {
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                // Add common headers
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .name("X-Request-ID")
                                .in("header")
                                .description("Unique request identifier for tracing")
                                .required(false)
                                .schema(new StringSchema().example("req-abc-123"))
                );
                operation.addParametersItem(
                        new io.swagger.v3.oas.models.parameters.Parameter()
                                .name("Accept-Language")
                                .in("header")
                                .description("Localization")
                                .required(false)
                                .schema(new StringSchema().example("id-ID")._default("id-ID"))
                );
            }));
        };
    }

    /**
     * Money schema for monetary amounts.
     */
    private Schema<?> moneySchema() {
        return new Schema<>()
                .type("object")
                .addProperty("amount", new Schema<>()
                        .type("number")
                        .format("decimal")
                        .example("1000000.00")
                        .description("Amount in decimal"))
                .addProperty("currency", new Schema<>()
                        .type("string")
                        .pattern("^[A-Z]{3}$")
                        .example("IDR")
                        ._default("IDR")
                        .description("ISO 4217 currency code"))
                .required(List.of("amount", "currency"));
    }

    /**
     * Page schema for pagination.
     */
    private Schema<?> pageSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("page", new IntegerSchema()
                        .example(0)
                        .description("Current page number (0-based)"))
                .addProperty("size", new IntegerSchema()
                        .example(20)
                        .description("Number of items per page"))
                .addProperty("totalElements", new IntegerSchema()
                        .example(150)
                        .description("Total number of elements"))
                .addProperty("totalPages", new IntegerSchema()
                        .example(8)
                        .description("Total number of pages"))
                .addProperty("hasNext", new Schema<>()
                        .type("boolean")
                        .example(true)
                        .description("Whether there is a next page"))
                .addProperty("hasPrevious", new Schema<>()
                        .type("boolean")
                        .example(false)
                        .description("Whether there is a previous page"));
    }

    /**
     * Error schema for error responses.
     */
    private Schema<?> errorSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("code", new StringSchema()
                        .example("INSUFFICIENT_BALANCE")
                        .description("Unique error code"))
                .addProperty("message", new StringSchema()
                        .example("Saldo tidak mencukupi untuk transaksi ini")
                        .description("Human-readable error message"))
                .addProperty("details", new Schema<>()
                        .type("array")
                        .description("Field-level validation errors"))
                .required(List.of("code", "message"));
    }
}
