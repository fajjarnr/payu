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
     * Build a blocking stub with the starter's client interceptors + a bounded
     * deadline (GRPC-011).
     */
    public <T extends AbstractStub<T>> T blockingStub(T stub, int deadlineSeconds) {
        T intercepted = stub;
        for (ClientInterceptor interceptor : clientInterceptors) {
            intercepted = intercepted.withInterceptors(interceptor);
        }
        return GrpcChannelSupport.withDeadline(intercepted, deadlineSeconds);
    }

    public List<ClientInterceptor> getClientInterceptors() {
        return clientInterceptors;
    }
}
