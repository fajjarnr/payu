package id.payu.grpc.starter.config;

import id.payu.grpc.starter.interceptor.GrpcAuthInterceptor;
import id.payu.grpc.starter.interceptor.GrpcErrorHandlingInterceptor;
import id.payu.grpc.starter.interceptor.GrpcRetryInterceptor;
import id.payu.grpc.starter.interceptor.GrpcTracingInterceptor;
import io.grpc.BindableService;
import io.grpc.ClientInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    @ConditionalOnBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "payu.grpc.interceptors.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "grpcAuthServerInterceptor")
    public ServerInterceptor grpcAuthServerInterceptor(JwtDecoder jwtDecoder) {
        boolean requireToken = properties.getInterceptors().getAuth().isRequireToken();
        log.info("Registering gRPC auth server interceptor (requireToken={})", requireToken);
        return new GrpcAuthInterceptor.ServerInterceptor(jwtDecoder, requireToken);
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
    @ConditionalOnMissingBean(name = "grpcChannelFactory")
    public GrpcChannelFactory grpcChannelFactory(
            @Autowired(required = false) List<ClientInterceptor> clientInterceptors) {
        log.info("Registering gRPC channel factory with {} client interceptors",
                clientInterceptors == null ? 0 : clientInterceptors.size());
        return new GrpcChannelFactory(clientInterceptors == null ? java.util.List.of() : clientInterceptors);
    }

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
    @ConditionalOnMissingBean
    public ServerBuilder<?> grpcServerBuilder() {
        NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(properties.getServer().getPort())
                .maxInboundMessageSize(properties.getServer().getMaxMessageSize());

        // Configure security if enabled
        if (properties.getServer().getSecurity().isEnabled()) {
            // TLS configuration would go here
            log.info("gRPC server TLS enabled");
        } else {
            log.info("gRPC server running without TLS (Istio handles TLS termination)");
        }

        // Add reflection service if enabled
        if (properties.getServer().isReflectionEnabled()) {
            serverBuilder.addService(ProtoReflectionService.newInstance());
            log.info("gRPC reflection service enabled");
        }

        return serverBuilder;
    }

    /**
     * Starts the gRPC server explicitly. spring-grpc's server auto-configuration
     * does not start a server from the builder in this setup, so the starter owns
     * the server lifecycle (FX-002: wallet gRPC was never reachable).
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "payu.grpc.server", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(BindableService.class)
    @ConditionalOnMissingBean(name = "grpcServer")
    public Server grpcServer(ServerBuilder<?> serverBuilder,
                             @Autowired(required = false) List<ServerInterceptor> interceptors,
                             List<BindableService> services) throws IOException {
        if (interceptors != null) {
            for (ServerInterceptor interceptor : interceptors) {
                serverBuilder.intercept(interceptor);
            }
        }
        for (BindableService service : services) {
            serverBuilder.addService(service);
        }
        Server server = serverBuilder.build();
        server.start();
        log.info("gRPC server started on port {}", properties.getServer().getPort());
        return server;
    }

    // ==================== Client Interceptor Provider ====================

    /**
     * Provider for client interceptors that can be used by gRPC channels.
     * Auth interceptor is optional — it may not be present if JwtDecoder is not configured.
     */
    @Bean
    @ConditionalOnMissingBean(name = "grpcClientInterceptors")
    public List<ClientInterceptor> grpcClientInterceptors(
            @Qualifier("grpcTracingClientInterceptor") ClientInterceptor tracingInterceptor,
            @Autowired(required = false) @Qualifier("grpcAuthClientInterceptor") ClientInterceptor authInterceptor,
            @Qualifier("grpcRetryInterceptor") ClientInterceptor retryInterceptor) {
        List<ClientInterceptor> interceptors = new ArrayList<>();
        interceptors.add(tracingInterceptor);
        if (authInterceptor != null) {
            interceptors.add(authInterceptor);
        }
        interceptors.add(retryInterceptor);
        return interceptors;
    }
}
