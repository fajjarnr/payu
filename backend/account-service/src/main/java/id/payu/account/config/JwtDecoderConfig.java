package id.payu.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);

    @Value("${payu.security.oauth2.issuer-uri:http://localhost:8080/realms/payu}")
    private String oidcIssuerUri;

    @Value("${payu.security.oauth2.jwk-set-uri:http://localhost:8080/realms/payu/protocol/openid-connect/certs}")
    private String oidcJwkSetUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JwtDecoder with issuer: {}", oidcIssuerUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(oidcJwkSetUri).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(oidcIssuerUri));

        return jwtDecoder;
    }
}
