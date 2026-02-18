package id.payu.logging.filter;

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

    private static final String TRACE_ID_KEY = "trace_id";
    private static final String SPAN_ID_KEY = "span_id";
    private static final String TRACE_FLAGS_KEY = "trace_flags";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        try {
            if (spanContext.isValid()) {
                MDC.put(TRACE_ID_KEY, spanContext.getTraceId());
                MDC.put(SPAN_ID_KEY, spanContext.getSpanId());
                MDC.put(TRACE_FLAGS_KEY, spanContext.getTraceFlags().toString());
            }

            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
            MDC.remove(TRACE_FLAGS_KEY);
        }
    }
}
