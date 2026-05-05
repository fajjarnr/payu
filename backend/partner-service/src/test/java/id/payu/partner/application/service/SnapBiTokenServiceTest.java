package id.payu.partner.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SnapBiTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private SnapBiTokenService tokenService;

    private Map<String, Object> redisStore;

    @BeforeEach
    public void setUp() {
        redisStore = new HashMap<>();
        ReflectionTestUtils.setField(tokenService, "tokenSecret", "test-secret-key-for-jwt-token-generation-validation-for-testing-only");
        ReflectionTestUtils.setField(tokenService, "expirationTimeMs", 900000L);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenAnswer(inv -> redisStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            redisStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), any(), any(Duration.class));
        doAnswer(inv -> {
            redisStore.remove(inv.getArgument(0));
            return null;
        }).when(redisTemplate).delete(anyString());

        tokenService.init();
    }

    @Test
    public void testGenerateAccessToken() {
        String clientId = "test-client-id";
        String partnerId = "123";
        String partnerName = "Test Partner";

        String token = tokenService.generateAccessToken(clientId, partnerId, partnerName);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    public void testValidateValidToken() {
        String clientId = "test-client-id";
        String partnerId = "123";
        String partnerName = "Test Partner";

        String token = tokenService.generateAccessToken(clientId, partnerId, partnerName);
        var claims = tokenService.validateToken(token);

        assertNotNull(claims);
        assertEquals(clientId, tokenService.getClientIdFromToken(token));
        assertEquals(partnerId, tokenService.getPartnerIdFromToken(token));
    }

    @Test
    public void testValidateInvalidToken() {
        var claims = tokenService.validateToken("invalid.token.here");

        assertNull(claims);
        assertNull(tokenService.getClientIdFromToken("invalid.token.here"));
    }

    @Test
    public void testRevokeToken() {
        String clientId = "test-client-id";
        String partnerId = "123";
        String partnerName = "Test Partner";

        String token = tokenService.generateAccessToken(clientId, partnerId, partnerName);
        assertNotNull(tokenService.validateToken(token));

        tokenService.revokeToken(token);
        assertNull(tokenService.validateToken(token));
    }
}
