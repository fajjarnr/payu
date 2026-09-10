package id.payu.grpc.starter.config;

import io.grpc.ClientInterceptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * GRPC-017b: the channel factory must expose the starter's client interceptors
 * so adapters attach them to every channel (raw channels never carried them).
 */
class GrpcChannelFactoryTest {

    @Test
    void exposesRegisteredClientInterceptors() {
        ClientInterceptor tracing = mock(ClientInterceptor.class);
        ClientInterceptor retry = mock(ClientInterceptor.class);
        GrpcChannelFactory factory = new GrpcChannelFactory(List.of(tracing, retry));

        assertThat(factory.getClientInterceptors()).containsExactly(tracing, retry);
    }

    @Test
    void buildsChannelFromStaticTarget() {
        GrpcChannelFactory factory = new GrpcChannelFactory(List.of());
        var channel = factory.channel("static://wallet-service:9090");
        assertThat(channel.authority()).isEqualTo("wallet-service:9090");
        channel.shutdownNow();
    }
    /**
     * RELAY-011: init-time withDeadlineAfter freezes. Factory must offer
     * deadline-free intercepted stub. Deadline applies per call.
     */
    @Test
    void interceptedStubHasNoFrozenDeadline() {
        GrpcChannelFactory factory = new GrpcChannelFactory(java.util.List.of());
        io.grpc.ManagedChannel ch = factory.channel("static://dummy:9090");
        try {
            DummyStub base = new DummyStub(ch, io.grpc.CallOptions.DEFAULT);
            DummyStub out = factory.interceptedStub(base);
            org.assertj.core.api.Assertions.assertThat(out.getCallOptions().getDeadline()).isNull();
            DummyStub timed = id.payu.grpc.starter.config.GrpcChannelSupport.withDeadline(out, 30);
            org.assertj.core.api.Assertions.assertThat(timed.getCallOptions().getDeadline()).isNotNull();
            long remain = timed.getCallOptions().getDeadline().timeRemaining(java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(remain).isBetween(25L, 30L);
        } finally {
            ch.shutdownNow();
        }
    }
    static final class DummyStub extends io.grpc.stub.AbstractBlockingStub<DummyStub> {
        DummyStub(io.grpc.Channel channel, io.grpc.CallOptions options) {
            super(channel, options);
        }
        @Override
        protected DummyStub build(io.grpc.Channel channel, io.grpc.CallOptions options) {
            return new DummyStub(channel, options);
        }
    }
}
