package id.payu.shared.restclient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

/**
 * Standardized REST client wrapper for external API calls in PayU platform.
 *
 * <p>Provides built-in resilience patterns (circuit breaker + retry) and
 * consistent error handling via {@link RestClientErrorHandler}.
 *
 * <p>Usage:
 * <pre>
 * &#64;Autowired
 * private PayuRestClient restClient;
 *
 * // Simple GET
 * ResponseEntity&lt;AccountDto&gt; response = restClient.get(
 *     "bi-fast", "/api/v1/accounts/123", AccountDto.class);
 *
 * // POST with body
 * ResponseEntity&lt;TransferResult&gt; result = restClient.post(
 *     "bi-fast", "/api/v1/transfers", requestBody, TransferResult.class);
 * </pre>
 *
 * <p>Each {@code serviceName} maps to a named circuit breaker and retry instance,
 * allowing per-service resilience tuning via configuration.
 */
public class PayuRestClient {

    private static final Logger log = LoggerFactory.getLogger(PayuRestClient.class);

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public PayuRestClient(RestClient restClient,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          RetryRegistry retryRegistry) {
        this.restClient = restClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * Performs a GET request to an external service with circuit breaker and retry.
     *
     * @param serviceName logical name for the external service (used for CB/retry isolation)
     * @param uri         the request URI (absolute or relative to configured base URL)
     * @param responseType the expected response type
     * @return the response entity
     */
    public <T> ResponseEntity<T> get(String serviceName, String uri, Class<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a GET request returning a parameterized type (e.g., List&lt;Item&gt;).
     */
    public <T> ResponseEntity<T> get(String serviceName, String uri,
                                     ParameterizedTypeReference<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a POST request to an external service with circuit breaker and retry.
     *
     * @param serviceName  logical name for the external service
     * @param uri          the request URI
     * @param body         the request body
     * @param responseType the expected response type
     * @return the response entity
     */
    public <T> ResponseEntity<T> post(String serviceName, String uri, Object body,
                                      Class<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a POST request returning a parameterized type.
     */
    public <T> ResponseEntity<T> post(String serviceName, String uri, Object body,
                                      ParameterizedTypeReference<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a PUT request to an external service with circuit breaker and retry.
     */
    public <T> ResponseEntity<T> put(String serviceName, String uri, Object body,
                                     Class<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.put()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a DELETE request to an external service with circuit breaker and retry.
     */
    public <T> ResponseEntity<T> delete(String serviceName, String uri, Class<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.delete()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Performs a POST request with custom headers.
     */
    public <T> ResponseEntity<T> postWithHeaders(String serviceName, String uri, Object body,
                                                  HttpHeaders headers, Class<T> responseType) {
        return executeWithResilience(serviceName, () ->
                restClient.post()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType));
    }

    /**
     * Wraps the given supplier with circuit breaker and retry from Resilience4j.
     * The circuit breaker is applied first, then retry wraps the circuit-broken call.
     */
    private <T> T executeWithResilience(String serviceName, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                "rest-" + serviceName);
        Retry retry = retryRegistry.retry("rest-" + serviceName);

        log.debug("Executing REST call to service '{}' [CB state: {}]",
                serviceName, circuitBreaker.getState());

        // Compose: retry wraps circuit breaker wraps supplier
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, supplier));

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("REST call to service '{}' failed after resilience handling: {}",
                    serviceName, e.getMessage());
            throw e;
        }
    }
}
