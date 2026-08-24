package id.payu.auth.adapter.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import id.payu.auth.config.DPoPProperties;
import id.payu.cache.service.DistributedCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RFC 9449 DPoP proof validator — minimal but spec-correct.
 * Validates typ, alg, jwk, signature, jti replay, htm/htu, iat window, ath.
 * Falls back to in-memory jti cache when DistributedCache unavailable (tests).
 */
@Slf4j
@Component
public class DPoPProofValidator {

    private final DPoPProperties properties;
    private final DistributedCache distributedCache;
    private final ConcurrentHashMap<String, Long> fallbackJti = new ConcurrentHashMap<>();

    @Autowired
    public DPoPProofValidator(DPoPProperties properties, @Autowired(required = false) DistributedCache distributedCache) {
        this.properties = properties;
        this.distributedCache = distributedCache;
    }

    public static class DPoPValidationException extends RuntimeException {
        public DPoPValidationException(String msg) { super(msg); }
        public DPoPValidationException(String msg, Throwable cause) { super(msg, cause); }
    }

    private static final String JTI_PREFIX = "dpop:jti:";

    /**
     * Validate DPoP proof JWT against request context and optional access token.
     *
     * @param dpopProof compact JWT (header typ=dpop+jwt)
     * @param method HTTP method (GET/POST etc)
     * @param requestUrl full URL (scheme://host/path) — compared to htu without query/fragment per spec
     * @param accessToken if present, ath claim must match hash(accessToken)
     * @return thumbprint of the JWK that signed the proof (base64url SHA-256)
     */
    public String validate(String dpopProof, String method, String requestUrl, String accessToken) {
        if (dpopProof == null || dpopProof.isBlank()) {
            throw new DPoPValidationException("DPoP proof is required");
        }
        JWSObject jws;
        try {
            jws = JWSObject.parse(dpopProof);
        } catch (ParseException e) {
            throw new DPoPValidationException("Invalid DPoP JWT format", e);
        }

        JWSHeader header = jws.getHeader();
        String typ = header.getType() != null ? header.getType().toString() : null;
        if (typ == null || !"dpop+jwt".equalsIgnoreCase(typ)) {
            throw new DPoPValidationException("DPoP typ must be dpop+jwt, got: " + typ);
        }
        JWSAlgorithm alg = header.getAlgorithm();
        if (alg == null || JWSAlgorithm.NONE.equals(alg)) {
            throw new DPoPValidationException("DPoP alg must not be none");
        }
        Set<String> allowed = Set.of(properties.getAllowedAlgs().split(","));
        if (!allowed.contains(alg.getName())) {
            throw new DPoPValidationException("DPoP alg not allowed: " + alg);
        }
        JWK jwk = header.getJWK();
        if (jwk == null) {
            throw new DPoPValidationException("DPoP header missing jwk");
        }
        JWSVerifier verifier;
        try {
            if (jwk instanceof RSAKey rsa) {
                verifier = new RSASSAVerifier(rsa);
            } else if (jwk instanceof ECKey ec) {
                verifier = new ECDSAVerifier(ec);
            } else if (jwk instanceof OctetKeyPair okp) {
                verifier = new Ed25519Verifier(okp);
            } else {
                throw new DPoPValidationException("Unsupported DPoP JWK type: " + jwk.getKeyType());
            }
        } catch (Exception e) {
            if (e instanceof DPoPValidationException) throw (DPoPValidationException) e;
            throw new DPoPValidationException("Failed to create DPoP verifier: " + e.getMessage(), e);
        }
        try {
            if (!jws.verify(verifier)) {
                throw new DPoPValidationException("DPoP signature invalid");
            }
        } catch (Exception e) {
            if (e instanceof DPoPValidationException) throw (DPoPValidationException) e;
            throw new DPoPValidationException("DPoP signature verification failed: " + e.getMessage(), e);
        }

        Map<String, Object> claims;
        try {
            claims = jws.getPayload().toJSONObject();
        } catch (Exception e) {
            throw new DPoPValidationException("DPoP payload not JSON", e);
        }

        String jti = (String) claims.get("jti");
        String htm = (String) claims.get("htm");
        String htu = (String) claims.get("htu");
        Number iatNum = (Number) claims.get("iat");
        if (jti == null || jti.isBlank()) throw new DPoPValidationException("DPoP jti required");
        if (htm == null || htm.isBlank()) throw new DPoPValidationException("DPoP htm required");
        if (htu == null || htu.isBlank()) throw new DPoPValidationException("DPoP htu required");
        if (iatNum == null) throw new DPoPValidationException("DPoP iat required");
        long iat = iatNum.longValue();
        long now = Instant.now().getEpochSecond();
        long skew = Math.abs(now - iat);
        if (skew > properties.getMaxIatSkewSeconds()) {
            throw new DPoPValidationException("DPoP iat outside window: skew=" + skew + "s");
        }
        if (!method.equalsIgnoreCase(htm)) {
            throw new DPoPValidationException("DPoP htm mismatch: expected " + method + " got " + htm);
        }
        String expectedHtu = stripQueryFragment(requestUrl);
        String claimedHtu = stripQueryFragment(htu);
        if (!expectedHtu.equals(claimedHtu)) {
            throw new DPoPValidationException("DPoP htu mismatch: expected " + expectedHtu + " got " + claimedHtu);
        }

        // jti replay — use DistributedCache if present else fallback map
        String jtiKey = JTI_PREFIX + jti;
        boolean replay;
        if (distributedCache != null) {
            String existing = distributedCache.get(jtiKey, String.class);
            replay = existing != null;
            if (!replay) {
                distributedCache.put(jtiKey, "1", Duration.ofSeconds(properties.getJtiTtlSeconds()));
            }
        } else {
            long expiry = now + properties.getJtiTtlSeconds();
            Long prev = fallbackJti.putIfAbsent(jti, expiry);
            replay = prev != null && prev > now;
            // clean expired occasionally
            if (fallbackJti.size() > 1000) {
                fallbackJti.entrySet().removeIf(e -> e.getValue() < now);
            }
        }
        if (replay) {
            throw new DPoPValidationException("DPoP jti replay detected: " + jti);
        }

        String ath = (String) claims.get("ath");
        if (accessToken != null) {
            String expectedAth = computeAth(accessToken);
            if (ath == null) {
                throw new DPoPValidationException("DPoP ath required when presenting access token");
            }
            if (!expectedAth.equals(ath)) {
                throw new DPoPValidationException("DPoP ath mismatch");
            }
        }

        try {
            // Nimbus 9.x has computeThumbprint(String) ; newer also has computeThumbprint()
            // Try string variant via reflection for compat
            try {
                return (String) jwk.getClass().getMethod("computeThumbprint", String.class)
                        .invoke(jwk, "SHA-256").toString();
            } catch (NoSuchMethodException nsme) {
                Object thumb = jwk.getClass().getMethod("computeThumbprint").invoke(jwk);
                return thumb.toString();
            }
        } catch (Exception e) {
            throw new DPoPValidationException("Failed to compute JWK thumbprint", e);
        }
    }

    public static String computeAth(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String stripQueryFragment(String url) {
        if (url == null) return "";
        int q = url.indexOf('?');
        int f = url.indexOf('#');
        int cut = -1;
        if (q >= 0 && f >= 0) cut = Math.min(q, f);
        else if (q >= 0) cut = q;
        else if (f >= 0) cut = f;
        return cut >= 0 ? url.substring(0, cut) : url;
    }
}
