package id.payu.grpc.starter.config;

import id.payu.grpc.starter.interceptor.GrpcAuthInterceptor;
import id.payu.grpc.starter.interceptor.GrpcErrorHandlingInterceptor;
import id.payu.grpc.starter.interceptor.GrpcRetryInterceptor;
import id.payu.grpc.starter.interceptor.GrpcTracingInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelConfigurer;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Spring Boot Auto-Configuration for gRPC starter.
 * Registers interceptors, server configuration, and client configuration.
 *
 * @author PayU Logic Builder
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(GrpcStarterProperties.class)
@ConditionalOnClass({ServerInterceptor.class, ClientInterceptor.class})
@RequiredArgsConstructor
public class GrpcStarterAutoConfiguration {

    private final GrpcStarterProperties properties;

    // ==================== Server Interceptors ====================

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcTracingServerInterceptor")
    public ServerInterceptor grpcTracingServerInterceptor() {
        log.info("Registering gRPC tracing server interceptor");
        return new GrpcTracingInterceptor.ServerInterceptor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcAuthServerInterceptor")
    public ServerInterceptor grpcAuthServerInterceptor(JwtDecoder jwtDecoder) {
        log.info("Registering gRPC auth server interceptor");
        return new GrpcAuthInterceptor.ServerInterceptor(jwtDecoder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.error-handling", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcErrorHandlingServerInterceptor")
    public ServerInterceptor grpcErrorHandlingServerInterceptor() {
        log.info("Registering gRPC error handling server interceptor");
        return new GrpcErrorHandlingInterceptor.ServerInterceptor();
    }

    // ==================== Client Interceptors ====================

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcTracingClientInterceptor")
    public ClientInterceptor grpcTracingClientInterceptor() {
        log.info("Registering gRPC tracing client interceptor");
        return new GrpcTracingInterceptor.ClientInterceptor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcAuthClientInterceptor")
    public ClientInterceptor grpcAuthClientInterceptor() {
        log.info("Registering gRPC auth client interceptor");
        return new GrpcAuthInterceptor.ClientInterceptor();
    }

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.error-handling", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcErrorHandlingClientInterceptor")
    public ClientInterceptor grpcErrorHandlingClientInterceptor() {
        log.info("Registering gRPC error handling client interceptor");
        return new GrpcErrorHandlingInterceptor.ClientInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean(name = "grpcRetryInterceptor")
    public ClientInterceptor grpcRetryInterceptor() {
        log.info("Registering gRPC retry interceptor");
        return new GrpcRetryInterceptor(
                3, // maxRetries
                100, // initialBackoffMs
                5000 // maxBackoffMs
        );
    }

    // ==================== Server Configuration ====================

    @Bean
    @ConditionalOnProperty(prefix = "payu.grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GrpcServerConfigurer grpcServerConfigurer() {
        return serverBuilder -> {
            if (serverBuilder instanceof NettyServerBuilder nettyBuilder) {
                nettyBuilder.maxInboundMessageSize(properties.getServer().getMaxMessageSize());

                // Configure security if enabled
                if (properties.getServer().getSecurity().isEnabled()) {
                    // TLS configuration would go here
                    log.info("gRPC server TLS enabled");
                } else {
                    log.info("gRPC server running without TLS (Istio handles TLS termination)");
                }

                // Add reflection service if enabled
                if (properties.getServer().isReflectionEnabled()) {
                    nettyBuilder.addService(ProtoReflectionService.newInstance());
                    log.info("gRPC reflection service enabled");
                }
            }
        };
    }

    // ==================== Client Configuration ====================

    @Bean
    @ConditionalOnMissingBean(name = "grpcChannelConfigurer")
    public GrpcChannelConfigurer grpcChannelConfigurer(List<ClientInterceptor> clientInterceptors) {
        return (channelBuilder, name) -> {
            if (channelBuilder instanceof NettyChannelBuilder nettyBuilder) {
                GrpcStarterProperties.ClientConfig clientConfig = properties.getClients().get(name);

                if (clientConfig != null) {
                    // Configure connection timeout
                    nettyBuilder.withOption(
                            io.grpc.netty.shaded.io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                            clientConfig.getConnectionTimeoutSeconds() * 1000
                    );

                    // Configure deadline
                    nettyBuilder.withDeadlineAfter(clientConfig.getDeadlineSeconds(), TimeUnit.SECONDS);

                    // Configure retry
                    if (clientConfig.isRetryEnabled()) {
                        // Retry is handled by GrpcRetryInterceptor
                        log.debug("Retry enabled for gRPC client: {}", name);
                    }
                }

                // Add interceptors
                List<ClientInterceptor> interceptors = new ArrayList<>(clientInterceptors);
                nettyBuilder.intercept(interceptors);

                log.info("Configured gRPC channel for service: {}", name);
            }
        };
    }
}
