package id.payu.logging.filter;

import id.payu.logging.config.PayuLoggingProperties;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.*;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Servlet filter that extracts OpenTelemetry trace and span IDs
 * and adds them to MDC for correlation in logs.
 */
public class TraceIdFilter implements Filter {

    private static final String DEFAULT_TRACE_ID_KEY = "trace_id";
    private static final String DEFAULT_SPAN_ID_KEY = "span_id";
    private static final String TRACE_FLAGS_KEY = "trace_flags";

    private final String traceIdKey;
    private final String spanIdKey;

    public TraceIdFilter() {
        this(null);
    }

    public TraceIdFilter(PayuLoggingProperties properties) {
        if (properties != null && properties.getTracing() != null) {
            this.traceIdKey = properties.getTracing().getTraceIdMdcKey();
            this.spanIdKey = properties.getTracing().getSpanIdMdcKey();
        } else {
            this.traceIdKey = DEFAULT_TRACE_ID_KEY;
            this.spanIdKey = DEFAULT_SPAN_ID_KEY;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        try {
            if (spanContext.isValid()) {
                MDC.put(traceIdKey, spanContext.getTraceId());
                MDC.put(spanIdKey, spanContext.getSpanId());
                MDC.put(TRACE_FLAGS_KEY, spanContext.getTraceFlags().toString());
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(traceIdKey);
            MDC.remove(spanIdKey);
            MDC.remove(TRACE_FLAGS_KEY);
        }
    }
}
