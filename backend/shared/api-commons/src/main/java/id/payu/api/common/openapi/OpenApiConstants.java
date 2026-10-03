package id.payu.api.common.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.List;

public final class OpenApiConstants {

    private OpenApiConstants() {
    }


    public static final class Tags {
        public static final String AUTH = "Authentication";
        public static final String ACCOUNTS = "Accounts";
        public static final String TRANSACTIONS = "Transactions";
        public static final String WALLETS = "Wallets";
        public static final String INVESTMENTS = "Investments";
        public static final String LENDING = "Lending";
        public static final String FX = "Foreign Exchange";
        public static final String KYC = "KYC Verification";
        public static final String BILLING = "Bill Payments";
        public static final String NOTIFICATIONS = "Notifications";
        public static final String PROMOTIONS = "Promotions";
        public static final String PARTNERS = "Partners";
        public static final String HEALTH = "Health Checks";
    }


    public static ApiResponse badRequest(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse unauthorized() {
        return new ApiResponse()
                .description("Authentication required")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))
                                        .example("""
                                                {
                                                  "code": "UNAUTHORIZED",
                                                  "message": "Token tidak valid atau sudah kadaluarsa"
                                                }
                                                """)));
    }

    public static ApiResponse forbidden() {
        return new ApiResponse()
                .description("Access denied")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))
                                        .example("""
                                                {
                                                  "code": "FORBIDDEN",
                                                  "message": "Anda tidak memiliki akses ke resource ini"
                                                }
                                                """)));
    }

    public static ApiResponse notFound() {
        return new ApiResponse()
                .description("Resource not found")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse conflict(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse unprocessableEntity(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse tooManyRequests() {
        return new ApiResponse()
                .description("Rate limit exceeded")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))
                                        .example("""
                                                {
                                                  "code": "RATE_LIMIT_EXCEEDED",
                                                  "message": "Terlalu banyak permintaan. Silakan coba lagi dalam 60 detik."
                                                }
                                                """)));
    }

    public static ApiResponse internalServerError() {
        return new ApiResponse()
                .description("Internal server error")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse badGateway(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static ApiResponse serviceUnavailable() {
        return new ApiResponse()
                .description("Service temporarily unavailable")
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/Error"))));
    }

    public static Operation addCommonResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        if (!responses.containsKey("400")) {
            responses.addApiResponse("400", badRequest("Invalid request"));
        }
        if (!responses.containsKey("401")) {
            responses.addApiResponse("401", unauthorized());
        }
        if (!responses.containsKey("500")) {
            responses.addApiResponse("500", internalServerError());
        }

        return operation;
    }
}
