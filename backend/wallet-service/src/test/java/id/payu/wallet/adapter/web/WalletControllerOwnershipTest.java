package id.payu.wallet.adapter.web;

import id.payu.wallet.application.exception.WalletNotFoundException;
import id.payu.wallet.domain.port.in.WalletUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalletControllerOwnershipTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void backendServiceTokenMaySettleAnyAccount() {
        WalletUseCase walletUseCase = mock(WalletUseCase.class);
        when(walletUseCase.getWalletByAccountId("ACC-001")).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("service-token")
                        .header("alg", "none")
                        .claim("azp", "payu-backend")
                        .subject("service-account-payu-backend")
                        .build()));

        WalletController controller = new WalletController(walletUseCase, "payu-backend");

        assertThrows(WalletNotFoundException.class, () -> controller.getBalance("ACC-001"));
    }
}
