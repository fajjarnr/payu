package id.payu.grpc.starter.interceptor;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GRPC-014: auth interceptor must reject anonymous calls when enforcement is
 * enabled, and keep allowing them by default (compat with token-less
 * service-to-service calls; mesh mTLS is the live control).
 */
class GrpcAuthInterceptorEnforcementTest {

    @SuppressWarnings("unchecked")
    private ServerCall<Object, Object> call() {
        ServerCall<Object, Object> call = mock(ServerCall.class);
        MethodDescriptor<Object, Object> method = mock(MethodDescriptor.class);
        when(method.getFullMethodName()).thenReturn("payu.wallet.WalletService/GetBalance");
        when(call.getMethodDescriptor()).thenReturn(method);
        return call;
    }

    private Metadata headers() {
        return new Metadata();
    }

    @SuppressWarnings("unchecked")
    private ServerCallHandler<Object, Object> next() {
        return mock(ServerCallHandler.class);
    }

    @Test
    void anonymousCallAllowedByDefault() {
        GrpcAuthInterceptor.ServerInterceptor interceptor =
                new GrpcAuthInterceptor.ServerInterceptor(mock(JwtDecoder.class), false);

        ServerCall<Object, Object> call = call();
        interceptor.interceptCall(call, headers(), next());

        verify(call, never()).close(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anonymousCallRejectedWhenEnforcementEnabled() {
        GrpcAuthInterceptor.ServerInterceptor interceptor =
                new GrpcAuthInterceptor.ServerInterceptor(mock(JwtDecoder.class), true);

        ServerCall<Object, Object> call = call();
        Metadata headers = headers();

        interceptor.interceptCall(call, headers, next());

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), org.mockito.ArgumentMatchers.eq(headers));
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }
}
