package id.payu.transaction.adapter.web;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GRPC-AUTH-001 (R4): the user scope handed to the use case must be the IAM
 * subject the token carries. The account-service resolves that subject to its
 * own user id (users.id) before answering with the caller's accounts — the
 * 'account_id' claim is an account id and never matches a user id.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerUserScopeTest {

    private static final String IAM_SUBJECT = "8d177115-af87-47d5-8b94-a09e7cfa55bc";
    private static final String ACCOUNT_ID_CLAIM = "750e8400-e29b-41d4-a716-446655440001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionUseCase transactionUseCase;

    @Test
    void getTransactionScopesTheCallerByTheTokenSubject() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(transactionUseCase.getTransaction(eq(transactionId), anyString()))
                .thenReturn(TransactionEntity.builder()
                        .id(transactionId)
                        .referenceNumber("TXN-SCOPE-001")
                        .senderAccountId(UUID.fromString(ACCOUNT_ID_CLAIM))
                        .amount(Money.idr("1000"))
                        .type(TransactionType.INTERNAL_TRANSFER)
                        .status(TransactionStatus.COMPLETED)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId)
                        .with(jwt().jwt(j -> j.subject(IAM_SUBJECT).claim("account_id", ACCOUNT_ID_CLAIM))
                                .authorities(() -> "read:transaction")))
                .andExpect(status().isOk());

        verify(transactionUseCase).getTransaction(transactionId, IAM_SUBJECT);
    }
}
