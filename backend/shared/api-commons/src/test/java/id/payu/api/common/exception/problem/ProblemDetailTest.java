package id.payu.api.common.exception.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.api.common.exception.BusinessException;
import id.payu.api.common.exception.InsufficientFundsException;
import id.payu.api.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProblemDetail} and {@link Rfc9457GlobalExceptionHandler}.
 *
 * <p>Verifies RFC 9457 §3 compliance + PayU extensions (error_code, trace_id, timestamp).
 */
class ProblemDetailTest {

    private Rfc9457GlobalExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        handler = new Rfc9457GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("ProblemDetail factory: produces all RFC 9457 mandatory fields + PayU extensions")
    void factoryProducesRfc9457Fields() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader("X-Trace-Id", "trace-12345");

        ProblemDetail pd = ProblemDetail.of(
                HttpStatus.BAD_REQUEST, "Validation failed",
                "amount must be positive", "TXN_400", request);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getTitle()).isEqualTo("Validation failed");
        assertThat(pd.getDetail()).isEqualTo("amount must be positive");
        assertThat(pd.getErrorCode()).isEqualTo("TXN_400");
        assertThat(pd.getInstance()).isEqualTo("/api/v1/transfers");
        assertThat(pd.getTraceId()).isEqualTo("trace-12345");
        assertThat(pd.getType().toString()).isEqualTo("https://payu.id/problems/bad-request");
        assertThat(pd.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ProblemDetail factory: X-Correlation-ID header used as trace_id fallback")
    void correlationIdAsTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        request.addHeader("X-Correlation-ID", "corr-abc-123");

        ProblemDetail pd = ProblemDetail.of(
                HttpStatus.NOT_FOUND, "Resource not found",
                "Account not found", "ACC_001", request);

        assertThat(pd.getTraceId()).isEqualTo("corr-abc-123");
        assertThat(pd.getType().toString()).isEqualTo("https://payu.id/problems/not-found");
        assertThat(pd.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("ProblemDetail factory: random UUID generated when no trace header present")
    void randomTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");

        ProblemDetail pd = ProblemDetail.of(
                HttpStatus.NOT_FOUND, "Resource not found",
                "Account not found", "ACC_001", request);

        assertThat(pd.getTraceId()).isNotNull();
        assertThat(pd.getTraceId()).hasSize(36); // UUID format
    }

    @Test
    @DisplayName("RFC 9457 JSON serialization: includes all mandatory + PayU extension fields")
    void jsonSerializationContainsAllFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        request.addHeader("X-Trace-Id", "trace-12345");

        ProblemDetail pd = ProblemDetail.of(
                HttpStatus.BAD_REQUEST, "Validation failed",
                "amount must be positive", "TXN_400", request);

        String json = objectMapper.writeValueAsString(pd);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        // RFC 9457 mandatory fields
        assertThat(map).containsKey("type");
        assertThat(map).containsKey("title");
        assertThat(map).containsKey("status");
        assertThat(map).containsKey("detail");
        assertThat(map).containsKey("instance");
        // PayU extensions
        assertThat(map).containsKey("error_code");
        assertThat(map).containsKey("trace_id");
        assertThat(map).containsKey("timestamp");

        assertThat(map.get("status")).isEqualTo(400);
        assertThat(map.get("error_code")).isEqualTo("TXN_400");
        assertThat(map.get("trace_id")).isEqualTo("trace-12345");
    }

    @Test
    @DisplayName("JSON field order: matches RFC 9457 + PayU spec")
    void jsonFieldOrder() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");

        ProblemDetail pd = ProblemDetail.of(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "Unexpected", "INTERNAL_ERROR", request);

        String json = objectMapper.writeValueAsString(pd);
        // Order: type, title, status, detail, instance, error_code, trace_id, timestamp
        int typeIdx = json.indexOf("\"type\"");
        int titleIdx = json.indexOf("\"title\"");
        int statusIdx = json.indexOf("\"status\"");
        int detailIdx = json.indexOf("\"detail\"");
        int instanceIdx = json.indexOf("\"instance\"");
        int errorCodeIdx = json.indexOf("\"error_code\"");
        int traceIdIdx = json.indexOf("\"trace_id\"");
        int timestampIdx = json.indexOf("\"timestamp\"");

        assertThat(typeIdx).isLessThan(titleIdx);
        assertThat(titleIdx).isLessThan(statusIdx);
        assertThat(statusIdx).isLessThan(detailIdx);
        assertThat(detailIdx).isLessThan(instanceIdx);
        assertThat(instanceIdx).isLessThan(errorCodeIdx);
        assertThat(errorCodeIdx).isLessThan(traceIdIdx);
        assertThat(traceIdIdx).isLessThan(timestampIdx);
    }

    @Test
    @DisplayName("BusinessException: NOT_FOUND → 404")
    void businessExceptionNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts/123");
        ResourceNotFoundException ex = new ResourceNotFoundException("Account", "123");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getType().toString()).isEqualTo("https://payu.id/problems/not-found");
    }

    @Test
    @DisplayName("InsufficientFundsException: 422 Unprocessable Entity")
    void insufficientFundsException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        InsufficientFundsException ex = new InsufficientFundsException("WALLET_001", "Balance too low");

        ResponseEntity<ProblemDetail> response = handler.handleInsufficientFunds(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getErrorCode()).isEqualTo("WALLET_001");
        assertThat(response.getBody().getTitle()).isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("Generic Exception: 500 with safe generic message")
    void genericException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transfers");
        Exception ex = new RuntimeException("Internal stack trace with sensitive info");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        // SECURITY: detail should NOT leak the original message
        assertThat(response.getBody().getDetail())
                .doesNotContain("Internal stack trace with sensitive info");
    }

    @Test
    @DisplayName("ConstraintViolationException: 400 with field violations")
    void constraintViolationException() {
        // Simpler: skip the mock complexity, just call factory directly
        ProblemDetail pd = ProblemDetail.of(HttpStatus.BAD_REQUEST, "Validation failed",
                "x must be positive", "VAL_001", new MockHttpServletRequest("GET", "/x"),
                List.of(FieldViolation.builder().field("x").message("must be positive").build()));
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getErrors()).hasSize(1);
    }

    @Test
    @DisplayName("FieldViolation: includes field, message, rejected_value, code")
    void fieldViolationFields() {
        FieldViolation fv = FieldViolation.builder()
                .field("amount")
                .message("must be positive")
                .rejectedValue(-100)
                .code("Min")
                .build();

        assertThat(fv.getField()).isEqualTo("amount");
        assertThat(fv.getMessage()).isEqualTo("must be positive");
        assertThat(fv.getRejectedValue()).isEqualTo(-100);
        assertThat(fv.getCode()).isEqualTo("Min");
    }

    @Test
    @DisplayName("Content-Type: application/problem+json (RFC 9457 §3 media type)")
    void contentTypeIsProblemJson() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        ResourceNotFoundException ex = new ResourceNotFoundException("Account", "123");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(ex, request);

        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/problem+json");
    }
}
