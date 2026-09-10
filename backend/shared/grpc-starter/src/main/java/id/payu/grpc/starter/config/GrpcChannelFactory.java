package id.payu.grpc.starter.config;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

import java.util.List;

/**
 * GRPC-017b/c: Spring-managed channel factory that attaches the starter's
 * client interceptors (tracing, auth, error-handling, retry) to every PayU
 * gRPC channel — raw channels previously never carried them. Prefer injecting
 * this bean over the static {@link GrpcChannelSupport} in Spring components.
 */
public class GrpcChannelFactory {

    private final List<ClientInterceptor> clientInterceptors;

    public GrpcChannelFactory(List<ClientInterceptor> clientInterceptors) {
        this.clientInterceptors = clientInterceptors;
    }

    public ManagedChannel channel(String target) {
        return GrpcChannelSupport.channel(target);
    }

    /**
     * Attach interceptors without deadline. Store result at init.
     * Apply deadline per call via {@link GrpcChannelSupport#withDeadline}.
     * RELAY-011: NEVER bake deadline into shared stub, freezes at creation.
     */
    public <T extends AbstractStub<T>> T interceptedStub(T stub) {
        T intercepted = stub;
        for (ClientInterceptor interceptor : clientInterceptors) {
            intercepted = intercepted.withInterceptors(interceptor);
        }
        return intercepted;
    }

    /**
     * Build a blocking stub with interceptors + bounded deadline (GRPC-011).
     * RELAY-011: NEVER store result in field. Deadline freezes at creation,
     * every call 30s after boot fails DEADLINE_EXCEEDED. Use
     * {@link #interceptedStub} at init plus per-call
     * {@link GrpcChannelSupport#withDeadline}.
     */
    public <T extends AbstractStub<T>> T blockingStub(T stub, int deadlineSeconds) {
        return GrpcChannelSupport.withDeadline(interceptedStub(stub), deadlineSeconds);
    }

    public List<ClientInterceptor> getClientInterceptors() {
        return clientInterceptors;
    }
}
