package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EscrowTransaction;
import id.payu.wallet.domain.model.EscrowTransaction.EscrowStatus;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.EscrowPersistencePort;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock
    private EscrowPersistencePort escrowPersistencePort;

    @Mock
    private WalletUseCase walletUseCase;

    @Mock
    private JournalUseCase journalUseCase;

    @Mock
    private WalletEventPublisherPort eventPublisher;

    @InjectMocks
    private EscrowService escrowService;

    private static final String BUYER_ACCOUNT = "buyer-account-001";
    private static final String SELLER_ACCOUNT = "seller-account-001";
    private static final String PARTNER_ID = "tokobapak";
    private static final BigDecimal AMOUNT = new BigDecimal("1000000");
    private static final BigDecimal FEE = new BigDecimal("25000");
    private static final String CURRENCY = "IDR";

    @Nested
    @DisplayName("createAndHoldEscrow")
    class CreateAndHoldEscrowTests {

        @Test
        @DisplayName("should create escrow, reserve buyer funds, and transition to HELD")
        void shouldCreateAndHoldSuccessfully() {
            // Arrange
            String reservationId = "reservation-" + UUID.randomUUID();
            when(walletUseCase.reserveBalance(eq(BUYER_ACCOUNT), eq(AMOUNT), anyString()))
                    .thenReturn(reservationId);
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            EscrowTransaction result = escrowService.createAndHoldEscrow(
                    BUYER_ACCOUNT, SELLER_ACCOUNT, PARTNER_ID,
                    AMOUNT, FEE, CURRENCY,
                    "order-123", "Test escrow", 48);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(EscrowStatus.HELD);
            assertThat(result.getBuyerAccountId()).isEqualTo(BUYER_ACCOUNT);
            assertThat(result.getSellerAccountId()).isEqualTo(SELLER_ACCOUNT);
            assertThat(result.getPartnerId()).isEqualTo(PARTNER_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.getFeeAmount()).isEqualByComparingTo(FEE);
            assertThat(result.getReservationId()).isEqualTo(reservationId);
            assertThat(result.getHeldAt()).isNotNull();
            assertThat(result.getExpiresAt()).isNotNull();

            // Verify wallet reservation was called
            verify(walletUseCase).reserveBalance(eq(BUYER_ACCOUNT), eq(AMOUNT), anyString());

            // Verify journal was created (DR 1100 / CR 2100)
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("Escrow hold"), eq("ESCROW"), anyString(),
                    entriesCaptor.capture(), eq("escrow-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
            assertThat(entries.get(0).getCoaCode()).isEqualTo("1100");
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
            assertThat(entries.get(1).getCoaCode()).isEqualTo("2100");

            verify(escrowPersistencePort).save(any(EscrowTransaction.class));
        }

        @Test
        @DisplayName("should default expiry to 72 hours when 0 is passed")
        void shouldDefaultExpiryTo72Hours() {
            when(walletUseCase.reserveBalance(anyString(), any(), anyString()))
                    .thenReturn("res-123");
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscrowTransaction result = escrowService.createAndHoldEscrow(
                    BUYER_ACCOUNT, SELLER_ACCOUNT, null,
                    AMOUNT, null, null,
                    null, "test", 0);

            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(71));
            assertThat(result.getCurrency()).isEqualTo("IDR");
            assertThat(result.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should propagate InsufficientBalanceException from wallet")
        void shouldPropagateInsufficientBalance() {
            when(walletUseCase.reserveBalance(eq(BUYER_ACCOUNT), eq(AMOUNT), anyString()))
                    .thenThrow(new InsufficientBalanceException(
                            BUYER_ACCOUNT, AMOUNT, BigDecimal.ZERO));

            assertThatThrownBy(() -> escrowService.createAndHoldEscrow(
                    BUYER_ACCOUNT, SELLER_ACCOUNT, PARTNER_ID,
                    AMOUNT, FEE, CURRENCY,
                    "order-123", "Test", 48))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(escrowPersistencePort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("releaseEscrow")
    class ReleaseEscrowTests {

        @Test
        @DisplayName("should transition HELD → RELEASED and create release journal")
        void shouldReleaseHeldEscrow() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildHeldEscrow(escrowId);

            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscrowTransaction result = escrowService.releaseEscrow(escrowId);

            assertThat(result.getStatus()).isEqualTo(EscrowStatus.RELEASED);
            assertThat(result.getReleasedAt()).isNotNull();

            // Verify journal: DR 2100 / CR 2200
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("Escrow release"), eq("ESCROW"), anyString(),
                    entriesCaptor.capture(), eq("escrow-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries.get(0).getCoaCode()).isEqualTo("2100");
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
            assertThat(entries.get(1).getCoaCode()).isEqualTo("2200");
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        }

        @Test
        @DisplayName("should throw when escrow not found")
        void shouldThrowWhenNotFound() {
            UUID escrowId = UUID.randomUUID();
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> escrowService.releaseEscrow(escrowId))
                    .isInstanceOf(EscrowNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when escrow is not in HELD status")
        void shouldThrowWhenNotHeld() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildCreatedEscrow(escrowId);
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));

            assertThatThrownBy(() -> escrowService.releaseEscrow(escrowId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HELD");
        }
    }

    @Nested
    @DisplayName("settleEscrow")
    class SettleEscrowTests {

        @Test
        @DisplayName("should transition RELEASED → SETTLED, credit merchant, and create settlement journal")
        void shouldSettleReleasedEscrow() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildReleasedEscrow(escrowId);
            BigDecimal netAmount = AMOUNT.subtract(FEE);

            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));
            when(walletUseCase.credit(eq(SELLER_ACCOUNT), eq(netAmount), anyString(), anyString()))
                    .thenReturn("tx-123");
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscrowTransaction result = escrowService.settleEscrow(escrowId);

            assertThat(result.getStatus()).isEqualTo(EscrowStatus.SETTLED);
            assertThat(result.getSettledAt()).isNotNull();

            // Verify merchant wallet credited with net amount
            verify(walletUseCase).credit(eq(SELLER_ACCOUNT), eq(netAmount),
                    eq(escrowId.toString()), contains("Escrow settlement"));

            // Verify journal: DR 2200 / CR 1100
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("Escrow settlement"), eq("ESCROW"), anyString(),
                    entriesCaptor.capture(), eq("escrow-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries.get(0).getCoaCode()).isEqualTo("2200");
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
            assertThat(entries.get(0).getAmount()).isEqualByComparingTo(netAmount);
            assertThat(entries.get(1).getCoaCode()).isEqualTo("1100");
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        }

        @Test
        @DisplayName("should throw when escrow is not RELEASED")
        void shouldThrowWhenNotReleased() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildHeldEscrow(escrowId);
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));

            assertThatThrownBy(() -> escrowService.settleEscrow(escrowId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RELEASED");
        }
    }

    @Nested
    @DisplayName("refundEscrow")
    class RefundEscrowTests {

        @Test
        @DisplayName("should refund HELD escrow — release reservation and credit buyer")
        void shouldRefundHeldEscrow() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildHeldEscrow(escrowId);

            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));
            when(walletUseCase.credit(eq(BUYER_ACCOUNT), eq(AMOUNT), anyString(), anyString()))
                    .thenReturn("tx-refund");
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            EscrowTransaction result = escrowService.refundEscrow(escrowId, "Buyer requested refund");

            assertThat(result.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
            assertThat(result.getRefundedAt()).isNotNull();
            assertThat(result.getRefundReason()).isEqualTo("Buyer requested refund");

            // Verify reservation released
            verify(walletUseCase).releaseReservation(escrow.getReservationId());

            // Verify buyer credited
            verify(walletUseCase).credit(eq(BUYER_ACCOUNT), eq(AMOUNT),
                    eq(escrowId.toString()), contains("Escrow refund"));

            // Verify journal: DR 2100 / CR 1100
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("Escrow refund"), eq("ESCROW"), anyString(),
                    entriesCaptor.capture(), eq("escrow-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries.get(0).getCoaCode()).isEqualTo("2100");
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
            assertThat(entries.get(1).getCoaCode()).isEqualTo("1100");
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.EntryType.CREDIT);
        }

        @Test
        @DisplayName("should throw when escrow status doesn't allow refund")
        void shouldThrowWhenStatusInvalid() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildCreatedEscrow(escrowId);
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));

            assertThatThrownBy(() -> escrowService.refundEscrow(escrowId, "test"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("processExpiredEscrows")
    class ProcessExpiredEscrowsTests {

        @Test
        @DisplayName("should auto-refund expired escrows")
        void shouldProcessExpiredEscrows() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction expiredEscrow = buildHeldEscrow(escrowId);
            // Make it appear expired
            expiredEscrow.setExpiresAt(LocalDateTime.now().minusHours(1));

            when(escrowPersistencePort.findExpiredHeldEscrows(any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(expiredEscrow));
            // expire() transitions to EXPIRED, then refundEscrow looks it up again
            when(escrowPersistencePort.save(any(EscrowTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(escrowPersistencePort.findById(escrowId))
                    .thenReturn(Optional.of(expiredEscrow));
            when(walletUseCase.credit(anyString(), any(), anyString(), anyString()))
                    .thenReturn("tx-refund");
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());

            escrowService.processExpiredEscrows();

            // Verify escrow was saved after expire()
            verify(escrowPersistencePort, atLeast(1)).save(any(EscrowTransaction.class));
        }

        @Test
        @DisplayName("should do nothing when no expired escrows")
        void shouldDoNothingWhenNoExpired() {
            when(escrowPersistencePort.findExpiredHeldEscrows(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            escrowService.processExpiredEscrows();

            verify(escrowPersistencePort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryTests {

        @Test
        @DisplayName("should get escrow by ID")
        void shouldGetEscrowById() {
            UUID escrowId = UUID.randomUUID();
            EscrowTransaction escrow = buildHeldEscrow(escrowId);
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.of(escrow));

            EscrowTransaction result = escrowService.getEscrow(escrowId);

            assertThat(result.getId()).isEqualTo(escrowId);
        }

        @Test
        @DisplayName("should throw when escrow not found by ID")
        void shouldThrowWhenNotFound() {
            UUID escrowId = UUID.randomUUID();
            when(escrowPersistencePort.findById(escrowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> escrowService.getEscrow(escrowId))
                    .isInstanceOf(EscrowNotFoundException.class);
        }

        @Test
        @DisplayName("should get escrows by buyer")
        void shouldGetByBuyer() {
            EscrowTransaction e1 = buildHeldEscrow(UUID.randomUUID());
            when(escrowPersistencePort.findByBuyerAccountId(BUYER_ACCOUNT))
                    .thenReturn(Collections.singletonList(e1));

            List<EscrowTransaction> result = escrowService.getEscrowsByBuyer(BUYER_ACCOUNT);

            assertThat(result).hasSize(1);
            verify(escrowPersistencePort).findByBuyerAccountId(BUYER_ACCOUNT);
        }

        @Test
        @DisplayName("should get escrows by seller")
        void shouldGetBySeller() {
            when(escrowPersistencePort.findBySellerAccountId(SELLER_ACCOUNT))
                    .thenReturn(Collections.emptyList());

            List<EscrowTransaction> result = escrowService.getEscrowsBySeller(SELLER_ACCOUNT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should get escrows by partner")
        void shouldGetByPartner() {
            EscrowTransaction e1 = buildHeldEscrow(UUID.randomUUID());
            when(escrowPersistencePort.findByPartnerId(PARTNER_ID))
                    .thenReturn(Arrays.asList(e1));

            List<EscrowTransaction> result = escrowService.getEscrowsByPartner(PARTNER_ID);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("EscrowTransaction domain model")
    class DomainModelTests {

        @Test
        @DisplayName("should calculate net amount correctly")
        void shouldCalculateNetAmount() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .amount(new BigDecimal("1000000"))
                    .feeAmount(new BigDecimal("25000"))
                    .build();

            assertThat(escrow.getNetAmount()).isEqualByComparingTo(new BigDecimal("975000"));
        }

        @Test
        @DisplayName("should handle null fee as zero")
        void shouldHandleNullFee() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .amount(new BigDecimal("1000000"))
                    .build();

            assertThat(escrow.getNetAmount()).isEqualByComparingTo(new BigDecimal("1000000"));
        }

        @Test
        @DisplayName("should detect expired escrow")
        void shouldDetectExpired() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .status(EscrowStatus.HELD)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .build();

            assertThat(escrow.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should not be expired when expiresAt is in future")
        void shouldNotBeExpiredWhenFuture() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .status(EscrowStatus.HELD)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            assertThat(escrow.isExpired()).isFalse();
        }

        @Test
        @DisplayName("should enforce lifecycle transitions")
        void shouldEnforceLifecycle() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .status(EscrowStatus.CREATED)
                    .build();

            // Can't release from CREATED
            assertThatThrownBy(escrow::release)
                    .isInstanceOf(IllegalStateException.class);

            // Can't settle from CREATED
            assertThatThrownBy(escrow::settle)
                    .isInstanceOf(IllegalStateException.class);

            // Can hold from CREATED
            escrow.hold("reservation-1");
            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.HELD);

            // Can't hold again
            assertThatThrownBy(() -> escrow.hold("reservation-2"))
                    .isInstanceOf(IllegalStateException.class);

            // Can release from HELD
            escrow.release();
            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.RELEASED);

            // Can settle from RELEASED
            escrow.settle();
            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.SETTLED);
        }

        @Test
        @DisplayName("should support refund from HELD")
        void shouldRefundFromHeld() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .status(EscrowStatus.CREATED)
                    .build();
            escrow.hold("res-1");
            escrow.refund("Buyer changed mind");

            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
            assertThat(escrow.getRefundReason()).isEqualTo("Buyer changed mind");
        }

        @Test
        @DisplayName("should support expire → refund path")
        void shouldSupportExpireRefundPath() {
            EscrowTransaction escrow = EscrowTransaction.builder()
                    .status(EscrowStatus.CREATED)
                    .build();
            escrow.hold("res-1");
            escrow.expire();
            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.EXPIRED);

            escrow.refund("Auto-refund: expired");
            assertThat(escrow.getStatus()).isEqualTo(EscrowStatus.REFUNDED);
        }
    }

    // --- Helper builders ---

    private EscrowTransaction buildCreatedEscrow(UUID id) {
        return EscrowTransaction.builder()
                .id(id)
                .buyerAccountId(BUYER_ACCOUNT)
                .sellerAccountId(SELLER_ACCOUNT)
                .partnerId(PARTNER_ID)
                .amount(AMOUNT)
                .feeAmount(FEE)
                .currency(CURRENCY)
                .status(EscrowStatus.CREATED)
                .externalReferenceId("order-123")
                .description("Test escrow")
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private EscrowTransaction buildHeldEscrow(UUID id) {
        return EscrowTransaction.builder()
                .id(id)
                .buyerAccountId(BUYER_ACCOUNT)
                .sellerAccountId(SELLER_ACCOUNT)
                .partnerId(PARTNER_ID)
                .amount(AMOUNT)
                .feeAmount(FEE)
                .currency(CURRENCY)
                .status(EscrowStatus.HELD)
                .externalReferenceId("order-123")
                .description("Test escrow")
                .reservationId("reservation-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .heldAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private EscrowTransaction buildReleasedEscrow(UUID id) {
        return EscrowTransaction.builder()
                .id(id)
                .buyerAccountId(BUYER_ACCOUNT)
                .sellerAccountId(SELLER_ACCOUNT)
                .partnerId(PARTNER_ID)
                .amount(AMOUNT)
                .feeAmount(FEE)
                .currency(CURRENCY)
                .status(EscrowStatus.RELEASED)
                .externalReferenceId("order-123")
                .description("Test escrow")
                .reservationId("reservation-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .heldAt(LocalDateTime.now().minusHours(1))
                .releasedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
