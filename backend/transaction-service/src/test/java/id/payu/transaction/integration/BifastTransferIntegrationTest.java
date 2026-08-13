package id.payu.transaction.integration;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.repository.TransactionJpaRepository;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.config.TestcontainersConfig;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.RgsServicePort;
import id.payu.transaction.domain.port.out.SknServicePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * QAMVP-009: BI-FAST transfer journey against real PostgreSQL (Testcontainers).
 * Persistence + transactional outbox are real; wallet/bifast/authorization
 * ports are mocked (external systems).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0", "spring.jpa.hibernate.ddl-auto=none"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-009 — BI-FAST transfer journey (real PostgreSQL + outbox)")
class BifastTransferIntegrationTest {

    @Autowired
    private InitiateTransferCommandHandler handler;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private WalletServicePort walletServicePort;

    @MockitoBean
    private BifastServicePort bifastServicePort;

    @MockitoBean
    private SknServicePort sknServicePort;

    @MockitoBean
    private RgsServicePort rgsServicePort;

    @MockitoBean
    private AuthorizationService authorizationService;

    private InitiateTransferCommand command() {
        return new InitiateTransferCommand(
                UUID.randomUUID(), "0123456789", Money.idr("100000"), "test bifast",
                TransactionType.BIFAST_TRANSFER, "123456", "device-1",
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), "011");
    }

    @Test
    @DisplayName("BI-FAST transfer persists transaction + outbox event atomically")
    void bifastTransferPersistsTransactionAndOutbox() throws Exception {
        when(walletServicePort.reserveBalance(any(), any(), any()))
                .thenReturn(id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-bi-fast").status("RESERVED").build());
        when(bifastServicePort.initiateTransfer(any()))
                .thenReturn(id.payu.transaction.dto.BifastTransferResponse.builder()
                        .referenceNumber("BIFAST-1").status("RESERVED").build());

        InitiateTransferCommandResult result = handler.handle(command());

        assertThat(result).isNotNull();
        TransactionEntity saved = transactionJpaRepository.findById(result.transactionId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getReservationId()).isEqualTo("res-bi-fast");

        assertThat(outboxRepository.findByAggregateId(result.transactionId().toString()))
                .as("BI-FAST transfer must publish exactly one outbox event")
                .hasSize(1);
    }

    @Test
    @DisplayName("BI-FAST provider failure compensates reservation and marks failed")
    void bifastFailureCompensatesAndMarksFailed() throws Exception {
        when(walletServicePort.reserveBalance(any(), any(), any()))
                .thenReturn(id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-fail").status("RESERVED").build());
        when(bifastServicePort.initiateTransfer(any()))
                .thenThrow(new RuntimeException("BI-FAST network down"));

        InitiateTransferCommandResult result = handler.handle(command());

        TransactionEntity saved = transactionJpaRepository.findById(result.transactionId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        org.mockito.Mockito.verify(walletServicePort).releaseBalance(
                any(), any(), org.mockito.ArgumentMatchers.eq("res-fail"), any());
    }
}
