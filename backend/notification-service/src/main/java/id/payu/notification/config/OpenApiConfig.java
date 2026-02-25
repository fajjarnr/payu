package id.payu.notification.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * OpenAPI configuration for PayU Notification Service.
 *
 * <p>Provides comprehensive API documentation including:
 * - Authentication via JWT bearer tokens
 * - Multi-channel notification delivery
 * - Error handling and response codes
 * - Rate limiting information
 * </p>
 */
@ApplicationScoped
@OpenAPIDefinition(
    info = @Info(
        title = "PayU Notification Service API",
        version = "1.0.0",
        description = """
            ## PayU Notification Service

            The Notification Service is responsible for delivering notifications to PayU users
            through multiple channels including Push, SMS, Email, and In-App notifications.

            ### Features

            - **Multi-channel Delivery**: Send notifications via Push, SMS, Email, or In-App
            - **Template Support**: Use pre-configured templates with dynamic data
            - **Delivery Tracking**: Monitor notification status and delivery confirmations
            - **Automatic Retry**: Configurable retry with exponential backoff
            - **Event-Driven**: Kafka integration for async processing

            ### Authentication

            All endpoints require JWT authentication. Include the token in the `Authorization` header:

            ```
            Authorization: Bearer <your-jwt-token>
            ```

            ### Rate Limiting

            - **Send Notification**: 100 requests per minute per user
            - **Get Notifications**: 200 requests per minute per user
            - **Mark as Read**: 200 requests per minute per user

            ### Error Handling

            The API uses standard HTTP status codes:

            - `200 OK` - Request succeeded
            - `201 Created` - Notification created successfully
            - `400 Bad Request` - Invalid request parameters
            - `401 Unauthorized` - Missing or invalid authentication
            - `403 Forbidden` - Insufficient permissions
            - `404 Not Found` - Resource not found
            - `429 Too Many Requests` - Rate limit exceeded
            - `500 Internal Server Error` - Server error
            """,
        termsOfService = "https://payu.fajjjar.my.id/terms",
        contact = @Contact(
            name = "PayU Platform Team",
            email = "platform@payu.fajjjar.my.id",
            url = "https://payu.fajjjar.my.id"
        ),
        license = @License(
            name = "Proprietary License",
            url = "https://payu.fajjjar.my.id/license",
            identifier = "PROPRIETARY"
        )
    ),
    servers = {
        @Server(
            description = "Local Development",
            url = "http://localhost:8086"
        ),
        @Server(
            description = "Development Environment",
            url = "https://notification-service-dev.payu.fajjjar.my.id"
        ),
        @Server(
            description = "Staging Environment",
            url = "https://notification-service-staging.payu.fajjjar.my.id"
        ),
        @Server(
            description = "Production Environment",
            url = "https://notification-service.payu.fajjjar.my.id"
        )
    },
    tags = {
        @Tag(
            name = "Notifications",
            description = "Operations for sending and managing notifications"
        ),
        @Tag(
            name = "Health",
            description = "Health check and monitoring endpoints"
        )
    },
    security = {
        @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(
            name = "bearerAuth"
        )
    }
)
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    description = "JWT authentication token from PayU auth-service. Include in Authorization header as 'Bearer <token>'",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Configuration is provided entirely through annotations
}
