package id.payu.auth.adapter.security;

import com.nimbusds.jwt.SignedJWT;
import id.payu.auth.application.metrics.BusinessMetrics;
import id.payu.auth.config.DPoPProperties;
import id.payu.cache.service.DistributedCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-0062 DPoP sender-constrained filter (RFC 9449).
 * ponytail: 5m jti TTL, single global nonce cache; per-client nonce store if scale needs it.
 */
@Slf4j
@Component
public class DPoPFilter extends OncePerRequestFilter {

    private final DPoPProofValidator validator;
    private final DPoPProperties properties;
    private final DistributedCache distributedCache;
    private final BusinessMetrics businessMetrics;

    @Autowired
    public DPoPFilter(DPoPProofValidator validator, DPoPProperties properties,
                      @Autowired(required = false) DistributedCache distributedCache,
                      @Autowired(required = false) BusinessMetrics businessMetrics) {
        this.validator = validator;
        this.properties = properties;
        this.distributedCache = distributedCache;
        this.businessMetrics = businessMetrics;
    }

    private static final String NONCE_PREFIX = "dpop:nonce:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String dpopHeader = request.getHeader("DPoP");

        boolean isDPoPAuth = authHeader != null && authHeader.startsWith("DPoP ");
        String token = null;
        if (isDPoPAuth) {
            token = authHeader.substring(5).trim();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        if (token == null && dpopHeader != null) {
            String path = request.getRequestURI();
            if (path.contains("/auth/refresh") || path.contains("/auth/callback") || path.contains("/auth/device")) {
                try {
                    validator.validate(dpopHeader, request.getMethod(), request.getRequestURL().toString(), null);
                } catch (DPoPProofValidator.DPoPValidationException e) {
                    log.warn("DPoP proof rejected for {} {}: {}", request.getMethod(), path, e.getMessage());
                    if (businessMetrics != null) businessMetrics.recordDpopInvalid();
                    response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\", error_description=\"" + sanitize(e.getMessage()) + "\"");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    maybeEmitNonce(response);
                    return;
                } catch (Exception e) {
                    log.warn("DPoP proof parse failed for {}: {}", path, e.getMessage());
                    if (businessMetrics != null) businessMetrics.recordDpopInvalid();
                    response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\"");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            chain.doFilter(request, response);
            return;
        }

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        boolean isBound = isBoundToken(token);
        boolean requireProof = isDPoPAuth || (isBound && properties.isRequireForBoundTokens());

        if (!requireProof) {
            if (dpopHeader != null) {
                try {
                    validator.validate(dpopHeader, request.getMethod(), request.getRequestURL().toString(), token);
                } catch (DPoPProofValidator.DPoPValidationException e) {
                    log.warn("Opportunistic DPoP proof invalid: {}", e.getMessage());
                    if (businessMetrics != null) businessMetrics.recordDpopInvalid();
                    response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\", error_description=\"" + sanitize(e.getMessage()) + "\"");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    maybeEmitNonce(response);
                    return;
                }
            }
            chain.doFilter(request, response);
            return;
        }

        if (dpopHeader == null || dpopHeader.isBlank()) {
            log.warn("DPoP proof required but missing for {} {} bound={} scheme={}", request.getMethod(), request.getRequestURI(), isBound, isDPoPAuth ? "DPoP" : "Bearer");
            if (businessMetrics != null) businessMetrics.recordDpopInvalid();
            response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\", error_description=\"DPoP proof required\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            maybeEmitNonce(response);
            return;
        }

        try {
            String thumbprint = validator.validate(dpopHeader, request.getMethod(), request.getRequestURL().toString(), token);
            if (isBound) {
                String jkt = extractJkt(token);
                if (jkt == null || !jkt.equals(thumbprint)) {
                    log.warn("DPoP cnf.jkt mismatch: token jkt={} thumbprint={}", mask(jkt), mask(thumbprint));
                    if (businessMetrics != null) businessMetrics.recordDpopInvalid();
                    response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\", error_description=\"cnf mismatch\"");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
            log.debug("DPoP proof validated for {} {}", request.getMethod(), request.getRequestURI());
        } catch (DPoPProofValidator.DPoPValidationException e) {
            log.warn("DPoP proof rejected for {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            if (businessMetrics != null) businessMetrics.recordDpopInvalid();
            response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\", error_description=\"" + sanitize(e.getMessage()) + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            maybeEmitNonce(response);
            return;
        } catch (Exception e) {
            log.warn("DPoP filter unexpected error: {}", e.getMessage());
            if (businessMetrics != null) businessMetrics.recordDpopInvalid();
            response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_dpop_proof\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private void maybeEmitNonce(HttpServletResponse response) {
        String nonce = UUID.randomUUID().toString();
        if (distributedCache != null) {
            distributedCache.put(NONCE_PREFIX + nonce, "1", Duration.ofSeconds(properties.getNonceTtlSeconds()));
        }
        if (businessMetrics != null) businessMetrics.recordDpopNonceRetry();
        response.setHeader("DPoP-Nonce", nonce);
    }

    private static boolean isBoundToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            Map<String, Object> payload = jwt.getPayload().toJSONObject();
            Object cnfObj = payload.get("cnf");
            if (cnfObj instanceof Map<?, ?> cnf) {
                Object jkt = cnf.get("jkt");
                return jkt instanceof String s && !s.isBlank();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractJkt(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            Map<String, Object> payload = jwt.getPayload().toJSONObject();
            Object cnfObj = payload.get("cnf");
            if (cnfObj instanceof Map<?, ?> cnf) {
                Object jkt = cnf.get("jkt");
                return jkt instanceof String s ? s : null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String mask(String v) {
        if (v == null) return "null";
        if (v.length() <= 8) return "***";
        return v.substring(0, 4) + "***" + v.substring(v.length() - 4);
    }

    private static String sanitize(String msg) {
        if (msg == null) return "invalid";
        return msg.replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }
}
