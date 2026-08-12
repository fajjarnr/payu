package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.dto.TransferRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletTransferControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String azp) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("azp", azp)
                        .subject("subject-" + azp)
                        .build()));
    }

    @Test
    void trustedServiceTransferInvokesSingleAtomicTransfer() {
        WalletUseCase walletUseCase = mock(WalletUseCase.class);
        when(walletUseCase.transfer("ACC-001", "ACC-002", new BigDecimal("100.0000"), "IDR",
                "snap-ref-1", "settlement")).thenReturn("ledger-tx-1");
        authenticate("payu-backend");

        WalletController controller = new WalletController(walletUseCase, "payu-backend");
        TransferRequest request = new TransferRequest();
        request.setSenderAccountId("ACC-001");
        request.setRecipientAccountId("ACC-002");
        request.setAmount(new BigDecimal("100.0000"));
        request.setCurrency("IDR");
        request.setReferenceId("snap-ref-1");
        request.setDescription("settlement");

        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.transfer(request);

        verify(walletUseCase).transfer("ACC-001", "ACC-002", new BigDecimal("100.0000"), "IDR",
                "snap-ref-1", "settlement");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("ledger-tx-1", response.getBody().getData().get("transactionId"));
    }

    @Test
    void untrustedCallerTransferIsDenied() {
        WalletUseCase walletUseCase = mock(WalletUseCase.class);
        authenticate("payu-web-app");

        WalletController controller = new WalletController(walletUseCase, "payu-backend");
        TransferRequest request = new TransferRequest();
        request.setSenderAccountId("ACC-001");
        request.setRecipientAccountId("ACC-002");
        request.setAmount(BigDecimal.ONE);
        request.setCurrency("IDR");
        request.setReferenceId("snap-ref-2");

        assertThrows(AccessDeniedException.class, () -> controller.transfer(request));
    }
}
