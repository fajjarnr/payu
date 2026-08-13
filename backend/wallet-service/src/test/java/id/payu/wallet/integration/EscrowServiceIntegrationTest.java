package id.payu.wallet.integration;

import id.payu.wallet.application.service.EscrowService;
import id.payu.wallet.config.TestcontainersConfig;
import id.payu.wallet.domain.model.EscrowStatus;
import id.payu.wallet.domain.model.EscrowTransaction;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.application.service.WalletService;
import id.payu.wallet.domain.port.out.EscrowPersistencePort;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * QAMVP-007: escrow money journey with real PostgreSQL (Testcontainers).
 * External wallet/journal/event ports are mocked; persistence is real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"payu.grpc.server.port=0"})
@Import(TestcontainersConfig.class)
@DisplayName("QAMVP-007 — escrow journey (real PostgreSQL)")
class EscrowServiceIntegrationTest {

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private EscrowPersistencePort escrowPersistencePort;

    @MockitoBean
    private WalletService walletUseCase;

    @MockitoBean
    private JournalUseCase journalUseCase;

    @MockitoBean
    private WalletEventPublisherPort eventPublisher;

    @Test
    @DisplayName("hold → release → settle persists state transitions")
    void escrowMoneyJourney() {
        when(walletUseCase.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn("res-1");

        EscrowTransaction held = escrowService.createAndHoldEscrow(
                "buyer-1", "seller-1", "partner-1",
                new BigDecimal("100000.0000"), new BigDecimal("2500.0000"),
                "IDR", "EXT-1", "escrow it", 24);

        assertThat(held.getStatus()).isEqualTo(EscrowStatus.HELD);
        assertThat(held.getReservationId()).isEqualTo("res-1");

        EscrowTransaction reloaded = escrowPersistencePort.findById(held.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EscrowStatus.HELD);

        EscrowTransaction released = escrowService.releaseEscrow(held.getId());
        assertThat(released.getStatus()).isEqualTo(EscrowStatus.RELEASED);

        EscrowTransaction settled = escrowService.settleEscrow(held.getId());
        assertThat(settled.getStatus()).isEqualTo(EscrowStatus.SETTLED);
        verify(walletUseCase).credit(eq("seller-1"), eq(new BigDecimal("97500.0000")), anyString(), anyString());

        verify(eventPublisher).publishEscrowHeld(any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    @DisplayName("held escrow refund releases reservation and does not double-credit")
    void refundFromHeldReleasesReservation() {
        when(walletUseCase.reserveBalance(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn("res-2");

        EscrowTransaction held = escrowService.createAndHoldEscrow(
                "buyer-2", "seller-2", "partner-1",
                new BigDecimal("50000.0000"), BigDecimal.ZERO,
                "IDR", "EXT-2", "escrow refund test", 24);

        EscrowTransaction refunded = escrowService.refundEscrow(held.getId(), "buyer wants refund");

        assertThat(refunded.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
        verify(walletUseCase).releaseReservation("res-2");
        verify(walletUseCase, org.mockito.Mockito.never())
                .credit(eq("buyer-2"), any(BigDecimal.class), any(), any());
    }

}
