package id.payu.transaction.config;

import id.payu.api.common.security.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
// BUG-BE-170: Required for @PreAuthorize to be enforced
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/actuator/**", "/transaction-service/actuator/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .addFilterBefore(securityHeadersFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Maps Keycloak JWT claims to Spring Security GrantedAuthority instances.
     * See wallet-service SecurityConfig for full documentation.
     */
    private Converter<Jwt, Collection<GrantedAuthority>> keycloakGrantedAuthoritiesConverter() {
        return jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();
            List<String> realmRoles = extractRealmRoles(jwt);

            // 1. Map realm_access.roles → ROLE_*
            for (String role : realmRoles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            // 2. Map resource_access.*.roles → ROLE_*
            extractResourceRoles(jwt).forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role))
            );

            // 3. Map scope → SCOPE_*
            String scope = jwt.getClaimAsString("scope");
            if (scope != null) {
                for (String s : scope.split(" ")) {
                    if (!s.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                    }
                }
            }

            // 4. Derive fine-grained permissions from coarse Keycloak roles
            boolean isUser = realmRoles.contains("default-roles-payu") || realmRoles.contains("user");
            boolean isAdmin = realmRoles.contains("admin");

            if (isUser) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(new SimpleGrantedAuthority("read:wallet"));
                authorities.add(new SimpleGrantedAuthority("write:wallet"));
                authorities.add(new SimpleGrantedAuthority("read:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:payment"));
                authorities.add(new SimpleGrantedAuthority("read:account"));
                authorities.add(new SimpleGrantedAuthority("write:account"));
            }

            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(new SimpleGrantedAuthority("read:wallet"));
                authorities.add(new SimpleGrantedAuthority("write:wallet"));
                authorities.add(new SimpleGrantedAuthority("read:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:transaction"));
                authorities.add(new SimpleGrantedAuthority("write:payment"));
                authorities.add(new SimpleGrantedAuthority("read:account"));
                authorities.add(new SimpleGrantedAuthority("write:account"));
            }

            return authorities;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractResourceRoles(Jwt jwt) {
        List<String> allRoles = new ArrayList<>();
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object value : resourceAccess.values()) {
                if (value instanceof Map) {
                    Map<String, Object> resource = (Map<String, Object>) value;
                    Object roles = resource.get("roles");
                    if (roles instanceof List) {
                        allRoles.addAll((List<String>) roles);
                    }
                }
            }
        }
        return allRoles;
    }
}
