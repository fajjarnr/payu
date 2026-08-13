package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.model.VaStatus;
import id.payu.transaction.domain.port.out.VirtualAccountPersistencePort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.VaCallbackRequest;
import id.payu.transaction.dto.VirtualAccountResponse;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VirtualAccountService banking callback settlement (MVP-003).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VirtualAccountService Tests")
class VirtualAccountServiceTest {

    @Mock
    private VirtualAccountPersistencePort virtualAccountPersistencePort;

    @Mock
    private WalletServicePort walletServicePort;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private VirtualAccountService virtualAccountService;

    private static VirtualAccountEntity pendingVa(String vaNumber, String settlementAccountId, BigDecimal amount) {
        return VirtualAccountEntity.builder()
                .id(UUID.randomUUID())
                .vaNumber(vaNumber)
                .bankCode("BCA")
                .bankName("BCA")
                .partnerId(UUID.randomUUID())
                .externalId("ext-1")
                .settlementAccountId(settlementAccountId)
                .amount(amount)
                .currency("IDR")
                .status(VaStatus.PENDING)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
    }

    @Nested
    @DisplayName("handleBankCallback settlement")
    class HandleBankCallbackTests {

        @Test
        @DisplayName("credits the settlement wallet and publishes payment.completed")
        void shouldCreditSettlementWalletAndPublishEvent() {
            String vaNumber = "8112345678";
            BigDecimal amount = new BigDecimal("100000.00");
            VirtualAccountEntity va = pendingVa(vaNumber, "ACC-SETTLE-01", amount);

            when(virtualAccountPersistencePort.findWithLockByVaNumber(vaNumber)).thenReturn(Optional.of(va));

            virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder()
                            .vaNumber(vaNumber)
                            .amount(amount)
                            .paymentReference("PAID-REF-1")
                            .build());

            verify(walletServicePort).creditBalance(eq("ACC-SETTLE-01"), eq(va.getId().toString()), eq(amount));
            verify(outboxService).createEvent(any(), any(), eq("VirtualAccountPaymentCompleted"), any(), any(), eq("payu.transaction.va-paid.v1"));
            verify(virtualAccountPersistencePort).savePaymentRecord(argThat(record ->
                    record.getVaNumber().equals(vaNumber)
                            && record.getAmount().compareTo(amount) == 0
                            && "PAID-REF-1".equals(record.getPaymentReference())));
            assertThat(va.getStatus()).isEqualTo(VaStatus.PAID);
        }

        @Test
        @DisplayName("missing settlement account rejects callback before mutation")
        void shouldRejectWhenNoSettlementAccount() {
            String vaNumber = "8998765432";
            BigDecimal amount = new BigDecimal("50000.00");
            VirtualAccountEntity va = pendingVa(vaNumber, null, amount);

            when(virtualAccountPersistencePort.findWithLockByVaNumber(vaNumber)).thenReturn(Optional.of(va));

            assertThatThrownBy(() -> virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder()
                            .vaNumber(vaNumber)
                            .amount(amount)
                            .paymentReference("PAID-REF-2")
                            .build()))
                    .isInstanceOf(IllegalStateException.class);

            verify(walletServicePort, never()).creditBalance(any(), any(), any());
            verify(virtualAccountPersistencePort, never()).savePaymentRecord(any());
            verifyNoInteractions(outboxService);
            assertThat(va.getStatus()).isEqualTo(VaStatus.PENDING);
        }

        @Test
        @DisplayName("outbox failure prevents remote credit")
        void shouldNotCreditWhenOutboxFails() {
            String vaNumber = "8666666666";
            BigDecimal amount = new BigDecimal("25000.00");
            VirtualAccountEntity va = pendingVa(vaNumber, "ACC-SETTLE-03", amount);

            when(virtualAccountPersistencePort.findWithLockByVaNumber(vaNumber)).thenReturn(Optional.of(va));
            when(outboxService.createEvent(
                    eq("VirtualAccount"), eq(va.getId().toString()),
                    eq("VirtualAccountPaymentCompleted"), anyMap(), isNull(),
                    eq("payu.transaction.va-paid.v1")))
                    .thenThrow(new IllegalStateException("outbox unavailable"));

            assertThatThrownBy(() -> virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder()
                            .vaNumber(vaNumber)
                            .amount(amount)
                            .paymentReference("PAID-REF-4")
                            .build()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("outbox unavailable");

            verify(walletServicePort, never()).creditBalance(any(), any(), any());
            // The payment record insert is same-tx with the outbox row — the
            // rollback discards both when the outbox publish fails.
            verify(virtualAccountPersistencePort).savePaymentRecord(any());
        }

        @Test
        @DisplayName("replayed callback on already-paid VA is a deterministic no-op without credit (IMP-2)")
        void shouldReturnExistingOnReplayedCallbackWithoutCredit() {
            String vaNumber = "8777111122";
            BigDecimal amount = new BigDecimal("75000.00");
            VirtualAccountEntity va = pendingVa(vaNumber, "ACC-SETTLE-02", amount);
            VirtualAccountEntity paidVa = pendingVa(vaNumber, "ACC-SETTLE-02", amount);
            paidVa.markPaid(amount, "PAID-REF-3");

            when(virtualAccountPersistencePort.findWithLockByVaNumber(vaNumber)).thenReturn(Optional.of(paidVa));

            VirtualAccountResponse response = virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder()
                            .vaNumber(vaNumber)
                            .amount(amount)
                            .paymentReference("PAID-REF-3")
                            .build());

            assertThat(response.getStatus()).isEqualTo(VaStatus.PAID.name());
            verify(walletServicePort, never()).creditBalance(any(), any(), any());
            verify(outboxService, never()).createEvent(any(), any(), any(), any(), any(), any());
            verify(virtualAccountPersistencePort, never()).savePaymentRecord(any());
        }

        @Test
        @DisplayName("two concurrent callbacks transition and settle exactly once (IMP-2)")
        void shouldSettleExactlyOnceAcrossTwoCallbacks() {
            String vaNumber = "8666000011";
            BigDecimal amount = new BigDecimal("10000.00");
            VirtualAccountEntity va = pendingVa(vaNumber, "ACC-SETTLE-04", amount);
            VirtualAccountEntity paidVa = pendingVa(vaNumber, "ACC-SETTLE-04", amount);
            paidVa.markPaid(amount, "PAID-REF-5");

            when(virtualAccountPersistencePort.findWithLockByVaNumber(vaNumber))
                    .thenReturn(Optional.of(va), Optional.of(paidVa));

            virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder().vaNumber(vaNumber).amount(amount).paymentReference("PAID-REF-5").build());
            virtualAccountService.handleBankCallback(
                    VaCallbackRequest.builder().vaNumber(vaNumber).amount(amount).paymentReference("PAID-REF-5").build());

            verify(walletServicePort, times(1)).creditBalance(any(), any(), any());
            verify(outboxService, times(1)).createEvent(any(), any(), any(), any(), any(), any());
            verify(virtualAccountPersistencePort, times(1)).savePaymentRecord(any());
        }
    }
}
