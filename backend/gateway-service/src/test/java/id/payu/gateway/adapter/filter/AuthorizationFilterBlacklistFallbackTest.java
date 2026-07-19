package id.payu.gateway.adapter.filter;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import id.payu.gateway.adapter.cache.HotRodCacheClient;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationFilterBlacklistFallbackTest {

    private AuthorizationFilter filter;
    private HotRodCacheClient cache;
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @BeforeEach
    void setUp() throws Exception {
        filter = new AuthorizationFilter();
        cache = mock(HotRodCacheClient.class);
        jwtProcessor = mock(ConfigurableJWTProcessor.class);

        setField("cache", cache);
        setField("jwtProcessor", jwtProcessor);
    }

    @Test
    @DisplayName("should continue JWT validation when Data Grid blacklist lookup fails")
    void shouldContinueJwtValidationWhenBlacklistLookupFails() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("user-123")
            .issuer("http://localhost:8080/realms/payu")
            .claim("account_id", "account-123")
            .claim("roles", List.of("ROLE_USER"))
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 60_000))
            .build();

        when(cache.get(anyString()))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Data Grid unavailable")));
        when(jwtProcessor.process(any(SignedJWT.class), isNull()))
            .thenReturn(claimsSet);

        Object userContext = invokeValidateToken(buildJwt());

        assertNotNull(userContext);
        Method getUserId = userContext.getClass().getDeclaredMethod("getUserId");
        getUserId.setAccessible(true);
        assertEquals("user-123", getUserId.invoke(userContext));
    }

    @Test
    @DisplayName("should reject blacklisted token before JWT validation")
    void shouldRejectBlacklistedToken() throws Exception {
        when(cache.get(anyString())).thenReturn(Uni.createFrom().item("1"));

        Object userContext = invokeValidateToken(buildJwt());

        assertNull(userContext);
    }

    private Object invokeValidateToken(String token) throws Exception {
        Method validateToken = AuthorizationFilter.class.getDeclaredMethod("validateToken", String.class);
        validateToken.setAccessible(true);
        return validateToken.invoke(filter, token);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = AuthorizationFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(filter, value);
    }

    private String buildJwt() {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(("{\"sub\":\"user-123\",\"iss\":\"http://localhost:8080/realms/payu\",\"exp\":4102444800,\"iat\":1700000000}")
                .getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("signature".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
