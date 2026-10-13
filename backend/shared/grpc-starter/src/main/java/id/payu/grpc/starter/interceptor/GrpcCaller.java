package id.payu.grpc.starter.interceptor;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.security.core.Authentication;

/**
 * Helper for gRPC handlers to enforce caller authentication.
 *
 * <p>The {@code GrpcAuthInterceptor.ServerInterceptor} puts the verified
 * {@link Authentication} into {@link GrpcAuthInterceptor#AUTH_CONTEXT_KEY} when
 * the caller presents a Bearer token. Read methods that disclose account, user,
 * or transaction data MUST call {@link #requireAuthenticated()} first so
 * anonymous callers get UNAUTHENTICATED instead of data.
 */
public final class GrpcCaller {

    private GrpcCaller() {
    }

    public static Authentication requireAuthenticated() {
        Authentication authentication = GrpcAuthInterceptor.AUTH_CONTEXT_KEY.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authentication required"));
        }
        return authentication;
    }

    /**
     * Fail-closed guard for handlers: replies UNAUTHENTICATED on the observer
     * and returns true when no verified caller is present (exceptions thrown
     * from handler bodies are wrapped as UNKNOWN by the framework, so guards
     * must answer on the observer instead of throwing).
     */
    public static boolean denyIfAnonymous(io.grpc.stub.StreamObserver<?> responseObserver) {
        Authentication authentication = GrpcAuthInterceptor.AUTH_CONTEXT_KEY.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Authentication required").asRuntimeException());
            return true;
        }
        return false;
    }
}
