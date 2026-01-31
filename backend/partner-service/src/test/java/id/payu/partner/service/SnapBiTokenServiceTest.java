package id.payu.partner.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SnapBiTokenServiceTest {

    @InjectMocks
    private SnapBiTokenService tokenService;

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
