package id.payu.transaction.adapter.web;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.interfaces.dto.InitiateTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QAMVP-014 (transaction — money flagship): unauthenticated → 401; no
 * {@code write:transaction} authority → 403 (RBAC); with authority → 201.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("QAMVP-014 — transaction security: 401/403 RBAC")
class TransferSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionUseCase transactionUseCase;

    private static final String BODY =
            "{\"senderAccountId\":\"%s\",\"recipientAccountNumber\":\"1234567890\","
            + "\"amount\":10000,\"currency\":\"IDR\",\"type\":\"INTERNAL_TRANSFER\",\"description\":\"test\"}";

    private void mockTransfer() {
        InitiateTransferCommandResult result = org.mockito.Mockito.mock(InitiateTransferCommandResult.class);
        when(result.toResponse()).thenReturn(new InitiateTransferResponse());
        when(result.transactionId()).thenReturn(UUID.randomUUID());
        when(transactionUseCase.initiateTransfer(any(InitiateTransferCommand.class))).thenReturn(result);
    }

    @Test
    @DisplayName("unauthenticated transfer is rejected with 401")
    void unauthenticatedTransferIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(String.format(BODY, UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated user without write:transaction authority is rejected with 403")
    void missingAuthorityIsRejected() throws Exception {
        mockTransfer();
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.claim("account_id", "acct-001")))
                        .contentType("application/json")
                        .content(String.format(BODY, UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated user with write:transaction authority gets 201")
    void authorizedTransferSucceeds() throws Exception {
        mockTransfer();
        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.claim("account_id", "acct-001"))
                                .authorities(() -> "write:transaction"))
                        .contentType("application/json")
                        .content(String.format(BODY, UUID.randomUUID())))
                .andExpect(status().isCreated());
    }
}
