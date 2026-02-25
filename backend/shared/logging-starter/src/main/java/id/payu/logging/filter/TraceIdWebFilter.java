package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive WebFilter that extracts OpenTelemetry trace and span IDs
 * and adds them to MDC. Equivalent of {@link TraceIdFilter} for WebFlux applications.
 */
public class TraceIdWebFilter implements WebFilter {

    private static final String TRACE_FLAGS_KEY = "trace_flags";

    private final String traceIdKey;
    private final String spanIdKey;

    public TraceIdWebFilter(PayuLoggingProperties properties) {
        if (properties != null && properties.getTracing() != null) {
            this.traceIdKey = properties.getTracing().getTraceIdMdcKey();
            this.spanIdKey = properties.getTracing().getSpanIdMdcKey();
        } else {
            this.traceIdKey = "trace_id";
            this.spanIdKey = "span_id";
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .doFirst(() -> {
                    Span currentSpan = Span.current();
                    SpanContext spanContext = currentSpan.getSpanContext();
                    if (spanContext.isValid()) {
                        MDC.put(traceIdKey, spanContext.getTraceId());
                        MDC.put(spanIdKey, spanContext.getSpanId());
                        MDC.put(TRACE_FLAGS_KEY, spanContext.getTraceFlags().toString());
                    }
                })
                .doFinally(signal -> {
                    MDC.remove(traceIdKey);
                    MDC.remove(spanIdKey);
                    MDC.remove(TRACE_FLAGS_KEY);
                });
    }
}
