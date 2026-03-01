package id.payu.grpc.starter.interceptor;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

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

    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;

    public GrpcRetryInterceptor() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_BACKOFF_MS, DEFAULT_MAX_BACKOFF_MS);
    }

    public GrpcRetryInterceptor(int maxRetries, long initialBackoffMs, long maxBackoffMs) {
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new RetryClientCall<>(method, callOptions, next);
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

            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseListener.onClose(Status.INTERNAL.withCause(e), new Metadata());
                return;
            }

            retryCount++;
            startCall();

            // Resend message if it was already sent
            if (message != null) {
                delegate.sendMessage(message);
                delegate.halfClose();
            }
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
