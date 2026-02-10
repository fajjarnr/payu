package id.payu.partner.adapter.web;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * OpenAPI constants and security scheme definitions for Partner Service.
 *
 * <p>This class defines common security schemes and annotation constants
 * used across all REST endpoints for OpenAPI documentation generation.</p>
 */
public final class OpenApiConstants {

    private OpenApiConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Security scheme name for Bearer token authentication.
     * Used for protected endpoints requiring JWT access tokens.
     */
    public static final String SECURITY_SCHEME_BEARER = "bearerAuth";

    /**
     * Security scheme name for API key authentication.
     * Used for partner integration endpoints.
     */
    public static final String SECURITY_SCHEME_API_KEY = "partnerApiKey";

    /**
     * Security scheme name for client signature authentication.
     * Used for SNAP BI endpoints requiring HMAC signature validation.
     */
    public static final String SECURITY_SCHEME_SIGNATURE = "signatureAuth";

    /**
     * Tag for Partner management endpoints.
     */
    public static final String TAG_PARTNER = "Partners";

    /**
     * Tag for Certificate management endpoints.
     */
    public static final String TAG_CERTIFICATE = "Certificates";

    /**
     * Tag for SNAP BI integration endpoints.
     */
    public static final String TAG_SNAP_BI = "SNAP BI";

    /**
     * Tag for Health check endpoints.
     */
    public static final String TAG_HEALTH = "Health";

    /**
     * Description for 200 OK response.
     */
    public static final String DESCRIPTION_200 = "Successful operation";

    /**
     * Description for 201 Created response.
     */
    public static final String DESCRIPTION_201 = "Resource created successfully";

    /**
     * Description for 204 No Content response.
     */
    public static final String DESCRIPTION_204 = "Resource deleted successfully";

    /**
     * Description for 400 Bad Request response.
     */
    public static final String DESCRIPTION_400 = "Invalid request parameters or validation error";

    /**
     * Description for 401 Unauthorized response.
     */
    public static final String DESCRIPTION_401 = "Authentication failed or token expired";

    /**
     * Description for 403 Forbidden response.
     */
    public static final String DESCRIPTION_403 = "Access forbidden - insufficient permissions";

    /**
     * Description for 404 Not Found response.
     */
    public static final String DESCRIPTION_404 = "Resource not found";

    /**
     * Description for 409 Conflict response.
     */
    public static final String DESCRIPTION_409 = "Request conflicts with current resource state";

    /**
     * Description for 500 Internal Server Error response.
     */
    public static final String DESCRIPTION_500 = "Internal server error occurred";
}
