package id.payu.grpc.starter.interceptor;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * gRPC client interceptor for automatic retry with exponential backoff.
 * Retries idempotent calls on transient failures.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
public class GrpcRetryInterceptor implements ClientInterceptor {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_BACKOFF_MS = 100;
    private static final long DEFAULT_MAX_BACKOFF_MS = 5000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    /**
     * Methods considered safe to retry (idempotent by nature).
     * Non-idempotent methods (e.g., those starting with "Create", "Insert")
     * should NOT be retried to avoid duplicate side effects.
     */
    private static final Set<MethodDescriptor.MethodType> IDEMPOTENT_METHOD_TYPES = Set.of(
            MethodDescriptor.MethodType.UNARY,
            MethodDescriptor.MethodType.SERVER_STREAMING
    );

    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final ScheduledExecutorService scheduler;

    public GrpcRetryInterceptor() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_BACKOFF_MS, DEFAULT_MAX_BACKOFF_MS);
    }

    public GrpcRetryInterceptor(int maxRetries, long initialBackoffMs, long maxBackoffMs) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grpc-retry-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        // BUG-SHARED-018: Only retry idempotent method types
        if (!isIdempotent(method)) {
            return next.newCall(method, callOptions);
        }

        return new RetryClientCall<>(method, callOptions, next);
    }

    /**
     * Determines if a gRPC method is safe to retry.
     * Only UNARY and SERVER_STREAMING are considered safe by default.
     */
    private <ReqT, RespT> boolean isIdempotent(MethodDescriptor<ReqT, RespT> method) {
        return IDEMPOTENT_METHOD_TYPES.contains(method.getType());
    }

    private class RetryClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {

        private final MethodDescriptor<ReqT, RespT> method;
        private final CallOptions callOptions;
        private final Channel channel;
        private ClientCall<ReqT, RespT> delegate;
        private Listener<RespT> responseListener;
        private Metadata headers;
        private ReqT message;
        private int retryCount = 0;
        private int pendingRequests = 0; // BUG-SHARED-019: track request() count

        RetryClientCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
            this.method = method;
            this.callOptions = callOptions;
            this.channel = channel;
        }

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
            this.responseListener = listener;
            this.headers = headers;
            startCall();
        }

        private void startCall() {
            this.delegate = channel.newCall(method, callOptions);
            delegate.start(new RetryListener(), headers);
        }

        @Override
        public void request(int numMessages) {
            pendingRequests += numMessages; // BUG-SHARED-019: accumulate request count
            delegate.request(numMessages);
        }

        @Override
        public void cancel(String message, Throwable cause) {
            delegate.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            delegate.halfClose();
        }

        @Override
        public void sendMessage(ReqT message) {
            this.message = message;
            delegate.sendMessage(message);
        }

        private boolean shouldRetry(Status status) {
            return retryCount < maxRetries && isRetryable(status);
        }

        private boolean isRetryable(Status status) {
            return switch (status.getCode()) {
                case UNAVAILABLE, RESOURCE_EXHAUSTED, DEADLINE_EXCEEDED -> true;
                case ABORTED, INTERNAL -> true;
                default -> false;
            };
        }

        private void scheduleRetry() {
            long backoffMs = Math.min(
                    initialBackoffMs * (long) Math.pow(BACKOFF_MULTIPLIER, retryCount),
                    maxBackoffMs
            );

            log.warn("Retrying gRPC call - method: {}, attempt: {}/{}, backoff: {}ms",
                    method.getFullMethodName(), retryCount + 1, maxRetries, backoffMs);

            retryCount++;

            // BUG-SHARED-017: Use ScheduledExecutorService instead of Thread.sleep()
            scheduler.schedule(() -> {
                startCall();

                // BUG-SHARED-019: Replay request() count on new delegate
                if (pendingRequests > 0) {
                    delegate.request(pendingRequests);
                }

                // Resend message if it was already sent
                if (message != null) {
                    delegate.sendMessage(message);
                    delegate.halfClose();
                }
            }, backoffMs, TimeUnit.MILLISECONDS);
        }

        private class RetryListener extends Listener<RespT> {

            @Override
            public void onHeaders(Metadata headers) {
                responseListener.onHeaders(headers);
            }

            @Override
            public void onMessage(RespT message) {
                responseListener.onMessage(message);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (!status.isOk() && shouldRetry(status)) {
                    scheduleRetry();
                } else {
                    if (!status.isOk() && retryCount > 0) {
                        log.error("gRPC call failed after {} retries - method: {}, status: {}",
                                retryCount, method.getFullMethodName(), status.getCode());
                    }
                    responseListener.onClose(status, trailers);
                }
            }

            @Override
            public void onReady() {
                responseListener.onReady();
            }
        }
    }
}
