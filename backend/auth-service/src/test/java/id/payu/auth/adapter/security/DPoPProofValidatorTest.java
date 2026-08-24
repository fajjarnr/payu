package id.payu.auth.adapter.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import id.payu.auth.config.DPoPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * ADR-0062 DPoP proof validator test — RFC 9449.
 * Tests valid proof, ath/cnf, htm/htu, iat window, jti replay.
 */
class DPoPProofValidatorTest {

    private DPoPProofValidator validator;
    private ECKey ecJwk;
    private DPoPProperties props;

    @BeforeEach
    void setUp() throws Exception {
        props = new DPoPProperties();
        props.setEnabled(true);
        props.setMaxIatSkewSeconds(300);
        props.setJtiTtlSeconds(300);
        props.setNonceTtlSeconds(300);
        props.setAllowedAlgs("ES256,ES384,ES512,PS256,RS256,EdDSA");
        validator = new DPoPProofValidator(props, null);
        ecJwk = new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256).generate();
    }

    private String createProof(String htm, String htu, String jti, long iat, String ath, ECKey key) throws Exception {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new com.nimbusds.jose.JOSEObjectType("dpop+jwt"))
                .jwk(key.toPublicJWK())
                .build();
        JWTClaimsSet.Builder cb = new JWTClaimsSet.Builder()
                .jwtID(jti)
                .claim("htm", htm)
                .claim("htu", htu)
                .claim("iat", iat);
        if (ath != null) cb.claim("ath", ath);
        SignedJWT jwt = new SignedJWT(header, cb.build());
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    private String ath(String token) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(token.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Test
    @DisplayName("valid ES256 proof validates and returns thumbprint")
    void validProof() throws Exception {
        String htm = "POST";
        String htu = "https://payu.co.id/api/v1/auth/refresh";
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().getEpochSecond();
        String fakeAccess = "header.payload.sig";
        String proof = createProof(htm, htu, jti, iat, ath(fakeAccess), ecJwk);
        String thumb = validator.validate(proof, htm, htu, fakeAccess);
        assertThat(thumb).isNotBlank();
        // thumbprint should equal computed
        String expected = ecJwk.toPublicJWK().computeThumbprint("SHA-256").toString();
        assertThat(thumb).isEqualTo(expected);
    }

    @Test
    @DisplayName("replay jti rejected")
    void replayRejected() throws Exception {
        String htm = "GET";
        String htu = "https://payu.co.id/api/v1/auth/validate";
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().getEpochSecond();
        String tok = "tok";
        String proof = createProof(htm, htu, jti, iat, ath(tok), ecJwk);
        validator.validate(proof, htm, htu, tok);
        // second use with same jti but need new proof (same jti) — will fail replay before signature?
        // Recreate same proof with same jti (signature same)
        assertThatThrownBy(() -> validator.validate(proof, htm, htu, tok))
                .isInstanceOf(DPoPProofValidator.DPoPValidationException.class)
                .hasMessageContaining("jti replay");
    }

    @Test
    @DisplayName("htm mismatch rejected")
    void htmMismatch() throws Exception {
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().getEpochSecond();
        String proof = createProof("POST", "https://payu.co.id/api/v1/auth/refresh", jti, iat, ath("t"), ecJwk);
        assertThatThrownBy(() -> validator.validate(proof, "GET", "https://payu.co.id/api/v1/auth/refresh", "t"))
                .isInstanceOf(DPoPProofValidator.DPoPValidationException.class)
                .hasMessageContaining("htm mismatch");
    }

    @Test
    @DisplayName("htu mismatch rejected")
    void htuMismatch() throws Exception {
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().getEpochSecond();
        String proof = createProof("POST", "https://payu.co.id/api/v1/auth/refresh", jti, iat, ath("t"), ecJwk);
        assertThatThrownBy(() -> validator.validate(proof, "POST", "https://payu.co.id/api/v1/other", "t"))
                .isInstanceOf(DPoPProofValidator.DPoPValidationException.class)
                .hasMessageContaining("htu mismatch");
    }

    @Test
    @DisplayName("ath mismatch rejected")
    void athMismatch() throws Exception {
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().getEpochSecond();
        String proof = createProof("GET", "https://payu.co.id/api/v1/auth/validate", jti, iat, "wrongAth", ecJwk);
        assertThatThrownBy(() -> validator.validate(proof, "GET", "https://payu.co.id/api/v1/auth/validate", "realToken"))
                .isInstanceOf(DPoPProofValidator.DPoPValidationException.class)
                .hasMessageContaining("ath mismatch");
    }

    @Test
    @DisplayName("expired iat rejected")
    void expiredIat() throws Exception {
        String jti = UUID.randomUUID().toString();
        long iat = Instant.now().minusSeconds(1000).getEpochSecond(); // >300 skew
        String proof = createProof("GET", "https://payu.co.id/api/v1/auth/validate", jti, iat, ath("t"), ecJwk);
        assertThatThrownBy(() -> validator.validate(proof, "GET", "https://payu.co.id/api/v1/auth/validate", "t"))
                .isInstanceOf(DPoPProofValidator.DPoPValidationException.class)
                .hasMessageContaining("iat outside");
    }
}
