package id.payu.transaction.integration;

import id.payu.transaction.application.service.TransactionService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.QrisServicePort;
import id.payu.transaction.domain.port.out.TransactionEventPublisherPort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("Transaction Integration Test")
class TransactionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionService transactionService;

    // Mock external dependencies
    @MockBean
    private WalletServicePort walletServicePort;
    @MockBean
    private BifastServicePort bifastServicePort;
    @MockBean
    private QrisServicePort qrisServicePort;
    @MockBean
    private TransactionEventPublisherPort eventPublisherPort;

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @Test
    @DisplayName("should persist transaction to real database")
    void shouldPersistTransactionToDatabase() {
        // Given
        InitiateTransferRequest request = new InitiateTransferRequest();
        request.setSenderAccountId(UUID.randomUUID());
        request.setRecipientAccountNumber("1234567890");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("IDR");
        request.setDescription("Integration Test Transfer");
        request.setType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER);

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().success(true).build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isNotNull();

        // Verify fetching from DB
        Transaction saved = transactionService.getTransaction(response.getTransactionId());
        assertThat(saved.getAmount()).isEqualByComparingTo(request.getAmount());
        assertThat(saved.getStatus()).isEqualTo(Transaction.TransactionStatus.VALIDATING); // As per service logic
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
         registry.add("spring.datasource.url", postgres::getJdbcUrl);
         registry.add("spring.datasource.username", postgres::getUsername);
         registry.add("spring.datasource.password", postgres::getPassword);
         registry.add("spring.flyway.enabled", () -> "true");
    }
}
