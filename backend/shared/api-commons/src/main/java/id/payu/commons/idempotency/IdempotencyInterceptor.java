package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Spring HandlerInterceptor for automatic idempotency handling.
 * <p>
 * This interceptor automatically processes requests marked with {@link Idempotent}
 * annotation, handling idempotency key extraction, validation, and response caching.
 * <p>
 * Features:
 * <ul>
 *   <li>Automatic Idempotency-Key header extraction</li>
 *   <li>Request fingerprinting for integrity validation</li>
 *   <li>Cached response return for duplicate requests</li>
 *   <li>Response storage for future duplicate detection</li>
 *   <li>In-progress request detection and handling</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 * <p>
 * Configuration:
 * <pre>
 * &#64;Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     private final IdempotencyInterceptor idempotencyInterceptor;
 *
 *     &#64;Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(idempotencyInterceptor)
 *             .addPathPatterns("/api/**");
 *     }
 * }
 * </pre>
 *
 * @see Idempotent
 * @see IdempotencyService
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUEST_BODY_SIZE = 1024 * 1024;
    private static final String IDEMPOTENCY_KEY_ATTR = "idempotency.key";
    private static final String IDEMPOTENCY_REQUEST_BODY_ATTR = "idempotency.requestBody";
    private static final String IDEMPOTENCY_HANDLED_ATTR = "idempotency.handled";
    private static final String IDEMPOTENCY_ANNOTATION_ATTR = "idempotency.annotation";

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check if method or class has @Idempotent annotation
        Idempotent annotation = getIdempotentAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }

        // Check if HTTP method should be handled
        if (!shouldHandleMethod(request, annotation)) {
            return true;
        }

        // Store annotation for postHandle
        request.setAttribute(IDEMPOTENCY_ANNOTATION_ATTR, annotation);

        // Extract idempotency key
        String idempotencyKey = request.getHeader(annotation.headerName());

        // Check if required
        if (annotation.required() && (idempotencyKey == null || idempotencyKey.trim().isEmpty())) {
            log.warn("Missing required Idempotency-Key header for {}", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Missing required header: " + annotation.headerName());
            return false;
        }

        // If not required and no key provided, skip idempotency
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return true;
        }

        // Validate key format
        try {
            IdempotencyKey.of(idempotencyKey);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Idempotency-Key format: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key format: " + e.getMessage());
            return false;
        }

        // Extract request body for fingerprinting
        Object requestBody = extractRequestBody(request);
        request.setAttribute(IDEMPOTENCY_REQUEST_BODY_ATTR, requestBody);
        request.setAttribute(IDEMPOTENCY_KEY_ATTR, idempotencyKey);

        // Check for existing entry
        Optional<IdempotencyEntry> existingEntry = idempotencyService.get(idempotencyKey, requestBody);

        if (existingEntry.isPresent()) {
            IdempotencyEntry entry = existingEntry.get();

            // Return cached response
            log.debug("Returning cached idempotency response for key '{}', status: {}",
                    idempotencyKey, entry.getHttpStatus());

            response.setStatus(entry.getHttpStatus());
            response.setContentType(entry.getContentType());

            if (entry.getResponseBody() != null) {
                response.getWriter().write(entry.getResponseBody());
            }

            request.setAttribute(IDEMPOTENCY_HANDLED_ATTR, true);
            return false; // Stop further processing
        }

        // Try to mark as in-progress
        boolean started = idempotencyService.startRequest(idempotencyKey, requestBody);
        if (!started) {
            // Another request is in progress
            log.warn("Concurrent idempotency request detected for key '{}'", idempotencyKey);
            sendErrorResponse(response, HttpStatus.CONFLICT,
                    "A request with this Idempotency-Key is currently being processed");
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        // Nothing to do here - we handle response caching after completion
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {

        // Check if request was already handled (cached response returned)
        if (Boolean.TRUE.equals(request.getAttribute(IDEMPOTENCY_HANDLED_ATTR))) {
            return;
        }

        // Check if we have idempotency context
        String idempotencyKey = (String) request.getAttribute(IDEMPOTENCY_KEY_ATTR);
        Idempotent annotation = (Idempotent) request.getAttribute(IDEMPOTENCY_ANNOTATION_ATTR);

        if (idempotencyKey == null || annotation == null) {
            return;
        }

        Object requestBody = request.getAttribute(IDEMPOTENCY_REQUEST_BODY_ATTR);

        // Store response based on outcome
        try {
            if (ex != null) {
                // Exception occurred - store error if configured
                if (annotation.cacheErrors()) {
                    HttpStatus status = determineErrorStatus(ex);
                    idempotencyService.storeError(idempotencyKey, requestBody, status, ex);
                }
            } else {
                // Successful response - store it
                storeSuccessfulResponse(request, response, idempotencyKey, requestBody);
            }
        } catch (Exception e) {
            // Don't fail the request because of idempotency storage issues
            log.error("Failed to store idempotency response for key '{}': {}",
                    idempotencyKey, e.getMessage());
        }
    }

    /**
     * Gets the @Idempotent annotation from method or class.
     */
    private Idempotent getIdempotentAnnotation(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();

        // Check method first
        Idempotent annotation = method.getAnnotation(Idempotent.class);
        if (annotation != null) {
            return annotation;
        }

        // Check class
        return handlerMethod.getBeanType().getAnnotation(Idempotent.class);
    }

    /**
     * Checks if the HTTP method should be handled for idempotency.
     */
    private boolean shouldHandleMethod(HttpServletRequest request, Idempotent annotation) {
        String httpMethod = request.getMethod();
        return Arrays.asList(annotation.methods()).contains(httpMethod);
    }

    /**
     * Extracts the request body from the handler method parameters.
     */
    private Object extractRequestBody(HttpServletRequest request) {
        try {
            byte[] body = request.getInputStream().readAllBytes();
            return createRequestFingerprint(request, canonicalizeBody(body));

        } catch (Exception e) {
            log.warn("Failed to extract request body for idempotency: {}", e.getMessage());
            return createRequestFingerprint(request, "body-read-failed");
        }
    }

    private String canonicalizeBody(byte[] body) throws IOException {
        if (body.length == 0) {
            return "";
        }
        if (body.length > MAX_REQUEST_BODY_SIZE) {
            try {
                return "sha256:" + java.util.Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(body));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            return json == null ? "" : canonicalize(json).toString();
        } catch (IOException invalidJson) {
            return new String(body, StandardCharsets.UTF_8).trim();
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            Iterator<String> names = node.fieldNames();
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            while (names.hasNext()) {
                String name = names.next();
                fields.put(name, node.get(name));
            }
            fields.forEach((name, value) -> sorted.set(name, canonicalize(value)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = objectMapper.createArrayNode();
            node.forEach(value -> sorted.add(canonicalize(value)));
            return sorted;
        }
        return node;
    }

    /** Creates a fingerprint from request metadata, identity, and canonical body. */
    private Map<String, Object> createRequestFingerprint(HttpServletRequest request, String body) {
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("uri", request.getRequestURI());
        fingerprint.put("method", request.getMethod());
        fingerprint.put("queryString", request.getQueryString());
        fingerprint.put("body", body);

        Map<String, String> identity = new LinkedHashMap<>();
        identity.put("principal", request.getUserPrincipal() == null
                ? "anonymous" : request.getUserPrincipal().getName());
        identity.put("tenant", request.getHeader("X-Tenant-Id"));
        identity.put("account", request.getHeader("X-Account-Id"));
        fingerprint.put("identity", identity);

        // Add relevant headers (excluding sensitive ones)
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                // Skip sensitive and non-relevant headers
                if (!isSensitiveHeader(name)) {
                    headers.put(name, request.getHeader(name));
                }
            }
        }
        fingerprint.put("headers", headers);

        return fingerprint;
    }

    /**
     * Checks if a header is sensitive and should not be included in fingerprint.
     */
    private boolean isSensitiveHeader(String name) {
        String lower = name.toLowerCase();
        return lower.contains("authorization") ||
               lower.contains("cookie") ||
               lower.contains("token") ||
               lower.contains("password") ||
               lower.contains("secret") ||
               lower.contains("idempotency-key"); // Already part of the key
    }

    /**
     * Stores a successful response for idempotency.
     */
    private void storeSuccessfulResponse(HttpServletRequest request, HttpServletResponse response,
                                        String idempotencyKey, Object requestBody) {
        try {
            int status = response.getStatus();

            // Only cache successful responses (2xx) unless cacheErrors is enabled
            if (status < 200 || status >= 300) {
                return;
            }

            // Read response body from wrapped response if available
            // Note: This requires a ContentCachingResponseWrapper
            if (response instanceof ContentCachingResponseWrapper cachingResponse) {
                byte[] content = cachingResponse.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = new String(content, response.getCharacterEncoding());
                    HttpStatus httpStatus = HttpStatus.valueOf(status);
                    idempotencyService.storeResponse(idempotencyKey, requestBody, httpStatus, responseBody);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to store successful response for idempotency: {}", e.getMessage());
        }
    }

    /**
     * Determines the HTTP status for an exception.
     */
    private HttpStatus determineErrorStatus(Exception ex) {
        // This could be enhanced with exception type mapping
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Sends an error response to the client.
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status,
                                   String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("status", status.value());

        objectMapper.writeValue(response.getWriter(), error);
    }

    /**
     * Wrapper class for caching response content.
     * This is a simplified version - in production, use Spring's ContentCachingResponseWrapper.
     */
    public static class ContentCachingResponseWrapper {
        // Placeholder - actual implementation would wrap HttpServletResponse
        // and cache the output stream content
        public byte[] getContentAsByteArray() {
            return new byte[0];
        }
    }
}
