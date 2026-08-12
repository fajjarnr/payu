package id.payu.grpc.starter.config;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRPC-011: channel helper must build usable channels and attach deadlines.
 */
class GrpcChannelSupportTest {

    @Test
    void parsesStaticTargetIntoChannel() {
        ManagedChannel channel = GrpcChannelSupport.channel("static://wallet-service:9090");
        assertThat(channel.authority()).isEqualTo("wallet-service:9090");
        channel.shutdownNow();
    }

    @Test
    void defaultsPortTo9090() {
        ManagedChannel channel = GrpcChannelSupport.channel("static://wallet-service");
        assertThat(channel.authority()).isEqualTo("wallet-service:9090");
        channel.shutdownNow();
    }
}
