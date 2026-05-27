package id.payu.quarkus.commons.exception;

import id.payu.quarkus.commons.response.ApiResponse;
import id.payu.quarkus.commons.response.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        return switch (ex) {
            case BusinessException business -> handleBusinessException(business);
            case ConstraintViolationException constraint -> handleConstraintViolation(constraint);
            case BadRequestException badRequest -> handleBadRequest(badRequest);
            case NotFoundException notFound -> handleNotFound(notFound);
            case WebApplicationException webApp -> handleWebApplication(webApp);
            default -> handleGenericException(ex);
        };
    }

    private Response handleBusinessException(BusinessException ex) {
        Response.Status status = determineHttpStatus(ex);
        log.warn("Business exception at {}: {}", getRequestPath(), ex.getMessage());

        if (ex instanceof ValidationException validation) {
            return Response.status(status)
                    .entity(ApiResponse.error(ex.getCode(), ex.getMessage(), validation.getFieldErrors()))
                    .build();
        }
        if (ex instanceof RateLimitExceededException rateLimit) {
            return Response.status(status)
                    .header("Retry-After", String.valueOf(rateLimit.getRetryAfterSeconds()))
                    .entity(ApiResponse.error(ex.getCode(), ex.getMessage()))
                    .build();
        }

        return Response.status(status)
                .entity(ApiResponse.error(ex.getCode(), ex.getMessage()))
                .build();
    }

    private Response handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation at {}: {}", getRequestPath(), ex.getMessage());

        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("VALIDATION_ERROR", "Request validation failed", fieldErrors))
                .build();
    }

    private Response handleBadRequest(BadRequestException ex) {
        log.warn("Bad request at {}: {}", getRequestPath(), ex.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.error("INVALID_REQUEST", "Bad request"))
                .build();
    }

    private Response handleNotFound(NotFoundException ex) {
        log.warn("Not found at {}: {}", getRequestPath(), ex.getMessage());
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error("NOT_FOUND", "The requested resource was not found"))
                .build();
    }

    private Response handleWebApplication(WebApplicationException ex) {
        log.warn("Web application exception at {}: {} - {}",
                getRequestPath(), ex.getClass().getSimpleName(), getSafeErrorMessage(ex));
        return Response.status(ex.getResponse().getStatus())
                .entity(ApiResponse.error("WEB_ERROR", "Request processing error"))
                .build();
    }

    private Response handleGenericException(Throwable ex) {
        log.error("Unexpected error at {}: {} - {}",
                getRequestPath(), ex.getClass().getSimpleName(), getSafeErrorMessage(ex));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred. Please try again later."))
                .build();
    }

    private Response.Status determineHttpStatus(BusinessException ex) {
        if (ex instanceof ResourceNotFoundException) return Response.Status.NOT_FOUND;
        if (ex instanceof ConflictException) return Response.Status.CONFLICT;
        if (ex instanceof RateLimitExceededException) return Response.Status.fromStatusCode(429);
        if (ex instanceof ExternalServiceException) return Response.Status.BAD_GATEWAY;
        if (ex instanceof ValidationException) return Response.Status.BAD_REQUEST;
        if (ex instanceof InsufficientFundsException) return Response.Status.fromStatusCode(422);

        String code = ex.getCode();
        if (code.contains("_VAL_") || code.contains("VALIDATION")) return Response.Status.BAD_REQUEST;
        if (code.contains("_BUS_")) return Response.Status.fromStatusCode(422);
        if (code.contains("_EXT_")) return Response.Status.BAD_GATEWAY;
        if (code.contains("NOT_FOUND")) return Response.Status.NOT_FOUND;
        if (code.contains("FORBIDDEN")) return Response.Status.FORBIDDEN;
        if (code.contains("UNAUTHORIZED")) return Response.Status.UNAUTHORIZED;

        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    private FieldError toFieldError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() != null
                ? violation.getPropertyPath().toString()
                : "unknown";
        if (field.contains(".")) {
            field = field.substring(field.lastIndexOf('.') + 1);
        }
        return FieldError.builder()
                .field(field)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }

    private String getRequestPath() {
        return uriInfo != null ? uriInfo.getPath() : "unknown";
    }

    private String getSafeErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null) return "No message";
        return message
                .replaceAll("password[^,]*", "password=***")
                .replaceAll("secret[^,]*", "secret=***")
                .replaceAll("token[^,]*", "token=***");
    }
}
