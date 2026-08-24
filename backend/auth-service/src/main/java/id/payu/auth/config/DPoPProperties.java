package id.payu.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ADR-0062 DPoP config — sender-constrained tokens (RFC 9449).
 * Ponytail: enabled by default, disable via payu.security.dpop.enabled=false for tests.
 */
@Data
@ConfigurationProperties(prefix = "payu.security.dpop")
public class DPoPProperties {
    private boolean enabled = true;
    /** Max age of DPoP iat (seconds) — RFC 9449 recommends ~5m */
    private long maxIatSkewSeconds = 300;
    /** TTL for jti replay cache */
    private long jtiTtlSeconds = 300;
    /** TTL for server nonce */
    private long nonceTtlSeconds = 300;
    /** Require DPoP proof when access token has cnf.jkt */
    private boolean requireForBoundTokens = true;
    /** Allowed JWS algs for DPoP proof (ES256 mandatory per RFC 9449) */
    private String allowedAlgs = "ES256,ES384,ES512,PS256,PS384,PS512,RS256,RS384,RS512,EdDSA";
}
