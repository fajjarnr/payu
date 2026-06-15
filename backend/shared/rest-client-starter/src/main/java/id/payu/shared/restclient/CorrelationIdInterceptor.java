package id.payu.shared.restclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Spring {@link ClientHttpRequestInterceptor} that propagates distributed tracing headers
 * on all outbound inter-service HTTP calls made via {@link PayuRestClient}.
 *
 * <p>The Gateway sets {@code X-Correlation-Id} on every inbound request. Without this
 * interceptor, that header is lost at the first service boundary, breaking distributed
 * tracing across the PayU microservice mesh (TRACE-001).</p>
 *
 * <p>Header propagation strategy:
 * <ol>
 *   <li>Read {@code X-Correlation-Id} from SLF4J MDC (populated by the service's own
 *       {@code CorrelationIdFilter} / {@code MDCFilter}).</li>
 *   <li>If not present in MDC, fall back to a freshly generated UUID so that every
 *       outbound call always carries a correlation ID.</li>
 *   <li>Also propagate {@code X-Request-Id} from MDC when available.</li>
 * </ol>
 *
 * <p>The interceptor is registered automatically by {@link RestClientAutoConfiguration}
 * and requires no additional configuration.
 */
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);

    /** MDC key used by the service's inbound filter to store the correlation ID. */
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    /** MDC key used by the service's inbound filter to store the request ID. */
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    /** Outbound HTTP header name for correlation ID (matches Gateway convention). */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Outbound HTTP header name for request ID. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {

        // Propagate correlation ID — generate a new one if not present in MDC
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("No correlationId in MDC — generated new one: {}", correlationId);
        }

        // Only set if not already present (respect upstream value)
        if (!request.getHeaders().containsHeader(CORRELATION_ID_HEADER)) {
            request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        }

        // Propagate request ID if available
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);
        if (requestId != null && !requestId.isBlank()
                && !request.getHeaders().containsHeader(REQUEST_ID_HEADER)) {
            request.getHeaders().set(REQUEST_ID_HEADER, requestId);
        }

        log.trace("Outbound {} {} — correlationId={}", request.getMethod(), request.getURI(), correlationId);

        return execution.execute(request, body);
    }
}
