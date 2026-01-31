package id.payu.backoffice.openapi;

import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.info.ContactImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.api.models.info.LicenseImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.security.SecurityRequirementImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;
import io.smallrye.openapi.api.models.servers.ServerImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI configuration for PayU Backoffice Service.
 * Configures Swagger UI, API documentation, and security schemes.
 */
@ApplicationScoped
public class OpenApiConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Produces
    @Singleton
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPIImpl();

        // Configure API Info
        InfoImpl info = new InfoImpl();
        info.setTitle("PayU Backoffice Service API");
        info.setVersion("v1");
        info.setDescription("""
                ## PayU Backoffice Service API

                Internal management portal for manual KYC review, fraud monitoring, and customer operations.

                ### Authentication

                All endpoints require authentication using a Bearer token (JWT) with backoffice admin role.

                ```http
                Authorization: Bearer <your-admin-token>
                ```

                ### Request Headers

                | Header | Required | Description |
                |--------|----------|-------------|
                | `Authorization` | Yes | Bearer token for authentication |
                | `X-Request-ID` | No | Unique request identifier for tracing |
                | `X-Admin-User` | No | Admin user identifier for audit trail |
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
                    "timestamp": "2026-01-31T10:30:00Z"
                  }
                }
                ```

                ### Rate Limiting

                - Default: 100 requests per minute
                - Strict (sensitive operations): 10 requests per minute

                ### Pagination

                List endpoints support pagination:

                ```
                GET /api/v1/backoffice/kyc-reviews?page=0&size=20
                ```

                - `page`: Page number (0-based, default: 0)
                - `size`: Items per page (default: 20, max: 100)
                """);

        ContactImpl contact = new ContactImpl();
        contact.setName("PayU API Support");
        contact.setEmail("api-support@payu.id");
        contact.setUrl("https://payu.id");
        info.setContact(contact);

        LicenseImpl license = new LicenseImpl();
        license.setName("Proprietary");
        license.setUrl("https://payu.id/terms");
        info.setLicense(license);

        openAPI.setInfo(info);

        // Configure Servers
        ServerImpl localServer = new ServerImpl();
        localServer.setUrl("http://localhost:8099");
        localServer.setDescription("Local Development");

        ServerImpl stagingServer = new ServerImpl();
        stagingServer.setUrl("https://staging-api.payu.id/backoffice");
        stagingServer.setDescription("Staging");

        ServerImpl prodServer = new ServerImpl();
        prodServer.setUrl("https://api.payu.id/backoffice");
        prodServer.setDescription("Production");

        openAPI.setServers(List.of(localServer, stagingServer, prodServer));

        // Configure Security Scheme
        SecuritySchemeImpl securityScheme = new SecuritySchemeImpl();
        securityScheme.setType(org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type.HTTP);
        securityScheme.setScheme("bearer");
        securityScheme.setBearerFormat("JWT");
        securityScheme.setDescription("JWT token with backoffice admin role from Keycloak");

        Map<String, org.eclipse.microprofile.openapi.models.security.SecurityScheme> securitySchemes = new HashMap<>();
        securitySchemes.put(SECURITY_SCHEME_NAME, securityScheme);
        openAPI.getComponents().setSecuritySchemes(securitySchemes);

        // Add global security requirement
        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();
        securityRequirement.addScheme(SECURITY_SCHEME_NAME);
        openAPI.addSecurityRequirement(securityRequirement);

        return openAPI;
    }
}
