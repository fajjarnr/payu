package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Reactive WebFilter that manages correlation ID for incoming requests.
 * Equivalent of {@link CorrelationIdFilter} for WebFlux applications.
 *
 * <p>Since MDC is thread-local and reactive pipelines switch threads,
 * this filter uses Reactor Context to propagate the correlation ID
 * and sets MDC in each signal (via contextWrite + deferContextual).</p>
 */
public class CorrelationIdWebFilter implements WebFilter {

    private final PayuLoggingProperties properties;

    public CorrelationIdWebFilter(PayuLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String headerName = properties.getCorrelation().getHeaderName();
        String mdcKey = properties.getCorrelation().getMdcKey();

        String correlationId = request.getHeaders().getFirst(headerName);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().replace("-", "");
        }

        // Add correlation ID to response header
        exchange.getResponse().getHeaders().set(headerName, correlationId);

        String finalCorrelationId = correlationId;
        String serviceName = properties.getServiceName() != null
                ? properties.getServiceName() : "unknown-service";

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx
                        .put(mdcKey, finalCorrelationId)
                        .put("service", serviceName))
                .doFirst(() -> {
                    MDC.put(mdcKey, finalCorrelationId);
                    MDC.put("service", serviceName);
                    MDC.put("service_version", properties.getServiceVersion());
                    MDC.put("environment", properties.getEnvironment());
                })
                .doFinally(signal -> {
                    MDC.remove(mdcKey);
                    MDC.remove("service");
                    MDC.remove("service_version");
                    MDC.remove("environment");
                });
    }
}
