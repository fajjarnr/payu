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
}
