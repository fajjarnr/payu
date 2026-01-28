package id.payu.transaction.integration;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.service.TransactionService;
import id.payu.transaction.domain.model.Money;
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
import org.junit.jupiter.api.Disabled;
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
@Disabled("Integration tests require Docker/Testcontainers. Enable with -Ddocker.available=true when Docker daemon is running.")
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

    // ==================== CQRS Pattern Tests ====================

    @Test
    @DisplayName("Should successfully initiate transfer using CQRS Command")
    void shouldSuccessfullyInitiateTransferUsingCQRSCommand() {
        // Given
        UUID senderAccountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        InitiateTransferCommand command = new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr("100000"),
                "CQRS test transfer",
                InitiateTransferRequest.TransactionType.BIFAST_TRANSFER,
                null,
                "device-123",
                null,
                userId.toString()
        );

        // When
        InitiateTransferCommandResult result = transactionService.initiateTransfer(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.transactionId()).isNotNull();
        assertThat(result.status()).isNotNull();
        assertThat(result.referenceNumber()).isNotEmpty();
    }

    // ==================== Money Value Object Tests ====================

    @Test
    @DisplayName("Money Value Object should enforce equality correctly")
    void moneyValueObjectShouldEnforceEqualityCorrectly() {
        // Given
        Money money1 = Money.idr("100000");
        Money money2 = Money.idr("100000");
        Money money3 = Money.idr("200000");

        // When & Then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1).hasSameHashCodeAs(money2);
        assertThat(money1).isNotEqualTo(money3);
    }

    @Test
    @DisplayName("Money should perform arithmetic operations correctly")
    void moneyShouldPerformArithmeticOperationsCorrectly() {
        // Given
        Money baseAmount = Money.idr("100000");

        // When
        Money added = baseAmount.add(Money.idr("50000"));
        Money subtracted = baseAmount.subtract(Money.idr("30000"));
        Money multiplied = baseAmount.multiply(2);

        // Then
        assertThat(added.getAmount()).isEqualTo(new BigDecimal("150000"));
        assertThat(subtracted.getAmount()).isEqualTo(new BigDecimal("70000"));
        assertThat(multiplied.getAmount()).isEqualTo(new BigDecimal("200000"));
    }

    @Test
    @DisplayName("Money should throw exception for negative operations")
    void moneyShouldThrowExceptionForNegativeOperations() {
        // Given
        Money money = Money.idr("100000");

        // When & Then
        assertThatThrownBy(() -> money.subtract(Money.idr("200000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> money.multiply(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiplier cannot be negative");
    }

    @Test
    @DisplayName("Money should validate currency in operations")
    void moneyShouldValidateCurrencyInOperations() {
        // Given
        Money idrMoney = Money.idr("100000");
        Money usdMoney = Money.usd("50");

        // When & Then
        assertThatThrownBy(() -> idrMoney.add(usdMoney))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    @DisplayName("Money should provide formatted display")
    void moneyShouldProvideFormattedDisplay() {
        // Given
        Money idr = Money.idr("1000000");
        Money usd = Money.usd("100");

        // When
        String idrFormatted = idr.format();
        String usdFormatted = usd.format();

        // Then
        assertThat(idrFormatted).contains("IDR");
        assertThat(idrFormatted).contains("1,000,000.00");
        assertThat(usdFormatted).contains("$");
        assertThat(usdFormatted).contains("100.00");
    }

    // ==================== Original Integration Tests ====================

    @Test
    @DisplayName("should persist transaction to real database")
    void shouldPersistTransactionToDatabase() {
        // Given
        UUID senderAccountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        InitiateTransferRequest request = new InitiateTransferRequest();
        request.setSenderAccountId(senderAccountId);
        request.setRecipientAccountNumber("1234567890");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("IDR");
        request.setDescription("Integration Test Transfer");
        request.setType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER);

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request, userId.toString());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isNotNull();

        // Verify fetching from DB
        Transaction saved = transactionService.getTransaction(response.getTransactionId(), userId.toString());
        assertThat(saved.getAmount()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Transaction.TransactionStatus.VALIDATING);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
