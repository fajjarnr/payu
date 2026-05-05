package id.payu.promotion.config;

import id.payu.promotion.domain.port.out.WalletServicePort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("scope", "read write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new MockJwtAuthenticationFilter(), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean(name = "walletGrpcAdapter")
    @Primary
    public WalletServicePort walletGrpcAdapter() {
        return (accountId, amount, referenceId, description) -> true;
    }

    @Bean(name = "walletRestAdapter")
    public WalletServicePort walletRestAdapter() {
        return (accountId, amount, referenceId, description) -> true;
    }

    @Bean
    public WalletServicePort walletServicePort() {
        return (accountId, amount, referenceId, description) -> true;
    }

    public static class MockJwtAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            String accountId = request.getHeader("X-Test-Account-Id");
            if (accountId == null || accountId.isBlank()) {
                accountId = "test-user";
            }
            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .claim("sub", accountId)
                    .claim("scope", "read write")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    jwt,
                    AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN", "ROLE_BACKOFFICE", "SCOPE_read", "SCOPE_write")
            );
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        }
    }
}
