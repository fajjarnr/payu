package id.payu.grpc.starter.interceptor;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * gRPC interceptor for distributed tracing.
 * Extracts trace ID from incoming requests and propagates it to outgoing requests.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(1)
public class GrpcTracingInterceptor {

    public static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> SPAN_ID_KEY =
            Metadata.Key.of("x-span-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> PARENT_SPAN_ID_KEY =
            Metadata.Key.of("x-parent-span-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> SAMPLED_KEY =
            Metadata.Key.of("x-sampled", Metadata.ASCII_STRING_MARSHALLER);

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";

    // gRPC Context keys for trace propagation
    public static final Context.Key<String> TRACE_ID_CONTEXT_KEY = Context.key("trace-id");
    public static final Context.Key<String> SPAN_ID_CONTEXT_KEY = Context.key("span-id");

    /**
     * Server interceptor that extracts trace context from incoming gRPC calls.
     */
    public static class ServerInterceptor implements io.grpc.ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {

            String traceId = headers.get(TRACE_ID_KEY);
            String spanId = headers.get(SPAN_ID_KEY);
            String sampled = headers.get(SAMPLED_KEY);

            // Generate new trace ID if not present
            if (traceId == null || traceId.isEmpty()) {
                traceId = generateTraceId();
            }

            // Generate new span ID if not present
            if (spanId == null || spanId.isEmpty()) {
                spanId = generateSpanId();
            }

            // Set in MDC for logging
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);

            // Create gRPC context with tracing info
            io.grpc.Context context = io.grpc.Context.current()
                    .withValue(TRACE_ID_CONTEXT_KEY, traceId)
                    .withValue(SPAN_ID_CONTEXT_KEY, spanId);

            log.debug("gRPC server call - traceId: {}, spanId: {}, method: {}",
                    traceId, spanId, call.getMethodDescriptor().getFullMethodName());

            ServerCall.Listener<ReqT> listener = Contexts.interceptCall(context, call, headers, next);

            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
                @Override
                public void onComplete() {
                    clearMdc();
                    super.onComplete();
                }

                @Override
                public void onCancel() {
                    clearMdc();
                    super.onCancel();
                }

                private void clearMdc() {
                    MDC.remove(MDC_TRACE_ID);
                    MDC.remove(MDC_SPAN_ID);
                }
            };
        }

        private String generateTraceId() {
            return UUID.randomUUID().toString().replace("-", "");
        }

        private String generateSpanId() {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    /**
     * Client interceptor that propagates trace context to outgoing gRPC calls.
     */
    public static class ClientInterceptor implements io.grpc.ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions,
                Channel next) {

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    // Get trace context from MDC or generate new
                    String traceId = MDC.get(MDC_TRACE_ID);
                    String spanId = MDC.get(MDC_SPAN_ID);

                    if (traceId != null && !traceId.isEmpty()) {
                        headers.put(TRACE_ID_KEY, traceId);
                    }

                    if (spanId != null && !spanId.isEmpty()) {
                        headers.put(SPAN_ID_KEY, spanId);
                    }

                    headers.put(SAMPLED_KEY, "1");

                    log.debug("gRPC client call - traceId: {}, method: {}",
                            traceId, method.getFullMethodName());

                    super.start(responseListener, headers);
                }
            };
        }
    }
}
