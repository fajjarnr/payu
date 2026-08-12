package id.payu.grpc.starter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GRPC-021(b): first test for grpc-starter — property binding contract of the
 * starter (payu.grpc.*) so config drift like GRPC-017 is caught at the source.
 */
class GrpcStarterPropertiesTest {

    private GrpcStarterProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("payu.grpc", GrpcStarterProperties.class)
                .orElseGet(GrpcStarterProperties::new);
    }

    @Test
    void bindsServerDefaults() {
        GrpcStarterProperties properties = bind(Map.of());
        assertThat(properties.getServer().isEnabled()).isTrue();
        assertThat(properties.getServer().getPort()).isEqualTo(9090);
        assertThat(properties.getServer().isReflectionEnabled()).isFalse();
        assertThat(properties.getServer().getMaxMessageSize()).isEqualTo(4194304);
    }

    @Test
    void bindsCustomServerPortAndReflection() {
        GrpcStarterProperties properties = bind(Map.of(
                "payu.grpc.server.enabled", "false",
                "payu.grpc.server.port", "9091",
                "payu.grpc.server.reflection-enabled", "true"
        ));
        assertThat(properties.getServer().isEnabled()).isFalse();
        assertThat(properties.getServer().getPort()).isEqualTo(9091);
        assertThat(properties.getServer().isReflectionEnabled()).isTrue();
    }

    @Test
    void bindsClientChannelConfig() {
        GrpcStarterProperties properties = bind(Map.of(
                "payu.grpc.clients.wallet-service.address", "static://wallet-service:9090",
                "payu.grpc.clients.wallet-service.deadline-seconds", "45",
                "payu.grpc.clients.wallet-service.max-retry-attempts", "5"
        ));
        GrpcStarterProperties.ClientConfig client = properties.getClients().get("wallet-service");
        assertThat(client).isNotNull();
        assertThat(client.getAddress()).isEqualTo("static://wallet-service:9090");
        assertThat(client.getDeadlineSeconds()).isEqualTo(45);
        assertThat(client.getMaxRetryAttempts()).isEqualTo(5);
    }

    @Test
    void bindsInterceptorToggles() {
        GrpcStarterProperties properties = bind(Map.of(
                "payu.grpc.interceptors.tracing.enabled", "false",
                "payu.grpc.interceptors.auth.enabled", "false"
        ));
        assertThat(properties.getInterceptors().getTracing().isEnabled()).isFalse();
        assertThat(properties.getInterceptors().getAuth().isEnabled()).isFalse();
    }
}
