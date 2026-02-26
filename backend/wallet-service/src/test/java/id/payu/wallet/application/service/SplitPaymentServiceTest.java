package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.*;
import id.payu.wallet.domain.model.SplitPaymentExecution.SplitExecutionStatus;
import id.payu.wallet.domain.model.SplitPaymentLeg.LegStatus;
import id.payu.wallet.domain.model.SplitPaymentRule.SplitType;
import id.payu.wallet.domain.model.SplitRecipient.RecipientType;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.SplitPaymentPersistencePort;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SplitPaymentServiceTest {

    @Mock
    private SplitPaymentPersistencePort persistencePort;

    @Mock
    private WalletUseCase walletUseCase;

    @Mock
    private JournalUseCase journalUseCase;

    @InjectMocks
    private SplitPaymentService splitPaymentService;

    private static final String PARTNER_ID = "tokobapak";
    private static final String PAYER_ACCOUNT = "payer-account-001";
    private static final String MERCHANT_A = "merchant-a-001";
    private static final String MERCHANT_B = "merchant-b-001";
    private static final String PLATFORM_ACCOUNT = "platform-fee-001";
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("1000000");
    private static final String CURRENCY = "IDR";

    private List<SplitRecipient> percentageRecipients;
    private List<SplitRecipient> fixedRecipients;

    @BeforeEach
    void setUp() {
        percentageRecipients = Arrays.asList(
                SplitRecipient.builder()
                        .recipientAccountId(MERCHANT_A)
                        .recipientLabel("Merchant A")
                        .type(RecipientType.MERCHANT)
                        .percentage(new BigDecimal("70"))
                        .priority(0)
                        .build(),
                SplitRecipient.builder()
                        .recipientAccountId(MERCHANT_B)
                        .recipientLabel("Merchant B")
                        .type(RecipientType.MERCHANT)
                        .percentage(new BigDecimal("25"))
                        .priority(1)
                        .build(),
                SplitRecipient.builder()
                        .recipientAccountId(PLATFORM_ACCOUNT)
                        .recipientLabel("Platform Fee")
                        .type(RecipientType.PLATFORM)
                        .percentage(new BigDecimal("5"))
                        .priority(2)
                        .build()
        );

        fixedRecipients = Arrays.asList(
                SplitRecipient.builder()
                        .recipientAccountId(MERCHANT_A)
                        .recipientLabel("Merchant A")
                        .type(RecipientType.MERCHANT)
                        .fixedAmount(new BigDecimal("700000"))
                        .priority(0)
                        .build(),
                SplitRecipient.builder()
                        .recipientAccountId(PLATFORM_ACCOUNT)
                        .recipientLabel("Platform Fee")
                        .type(RecipientType.PLATFORM)
                        .fixedAmount(new BigDecimal("300000"))
                        .priority(1)
                        .build()
        );
    }

    // ═══════════════════════════════════════════════════════
    //  Rule Management Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("createRule")
    class CreateRuleTests {

        @Test
        @DisplayName("should create a percentage split rule successfully")
        void shouldCreatePercentageRuleSuccessfully() {
            when(persistencePort.saveRule(any(SplitPaymentRule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentRule result = splitPaymentService.createRule(
                    PARTNER_ID, "Standard 70/25/5", SplitType.PERCENTAGE,
                    CURRENCY, percentageRecipients);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getPartnerId()).isEqualTo(PARTNER_ID);
            assertThat(result.getRuleName()).isEqualTo("Standard 70/25/5");
            assertThat(result.getSplitType()).isEqualTo(SplitType.PERCENTAGE);
            assertThat(result.isActive()).isTrue();
            assertThat(result.getRecipients()).hasSize(3);

            verify(persistencePort).saveRule(any(SplitPaymentRule.class));
        }

        @Test
        @DisplayName("should create a fixed split rule successfully")
        void shouldCreateFixedRuleSuccessfully() {
            when(persistencePort.saveRule(any(SplitPaymentRule.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentRule result = splitPaymentService.createRule(
                    PARTNER_ID, "Fixed Split", SplitType.FIXED,
                    CURRENCY, fixedRecipients);

            assertThat(result).isNotNull();
            assertThat(result.getSplitType()).isEqualTo(SplitType.FIXED);
            assertThat(result.getRecipients()).hasSize(2);
            verify(persistencePort).saveRule(any(SplitPaymentRule.class));
        }

        @Test
        @DisplayName("should reject percentage rule not summing to 100%")
        void shouldRejectInvalidPercentageSum() {
            List<SplitRecipient> badRecipients = Arrays.asList(
                    SplitRecipient.builder()
                            .recipientAccountId(MERCHANT_A)
                            .recipientLabel("Merchant A")
                            .percentage(new BigDecimal("60"))
                            .priority(0)
                            .build(),
                    SplitRecipient.builder()
                            .recipientAccountId(MERCHANT_B)
                            .recipientLabel("Merchant B")
                            .percentage(new BigDecimal("30"))
                            .priority(1)
                            .build()
            );

            assertThatThrownBy(() -> splitPaymentService.createRule(
                    PARTNER_ID, "Bad Rule", SplitType.PERCENTAGE,
                    CURRENCY, badRecipients))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("100%");

            verify(persistencePort, never()).saveRule(any());
        }

        @Test
        @DisplayName("should reject rule with no recipients")
        void shouldRejectEmptyRecipients() {
            assertThatThrownBy(() -> splitPaymentService.createRule(
                    PARTNER_ID, "Empty Rule", SplitType.PERCENTAGE,
                    CURRENCY, List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one recipient");
        }
    }

    @Nested
    @DisplayName("getRule & getRulesByPartner")
    class QueryRuleTests {

        @Test
        @DisplayName("should return rule by ID")
        void shouldReturnRuleById() {
            UUID ruleId = UUID.randomUUID();
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .id(ruleId).partnerId(PARTNER_ID).active(true)
                    .splitType(SplitType.PERCENTAGE).recipients(percentageRecipients)
                    .build();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));

            SplitPaymentRule result = splitPaymentService.getRule(ruleId);
            assertThat(result.getId()).isEqualTo(ruleId);
        }

        @Test
        @DisplayName("should throw when rule not found")
        void shouldThrowWhenRuleNotFound() {
            UUID ruleId = UUID.randomUUID();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> splitPaymentService.getRule(ruleId))
                    .isInstanceOf(SplitPaymentNotFoundException.class);
        }

        @Test
        @DisplayName("should return rules by partner")
        void shouldReturnRulesByPartner() {
            SplitPaymentRule r1 = SplitPaymentRule.builder().id(UUID.randomUUID()).partnerId(PARTNER_ID).build();
            SplitPaymentRule r2 = SplitPaymentRule.builder().id(UUID.randomUUID()).partnerId(PARTNER_ID).build();
            when(persistencePort.findRulesByPartnerId(PARTNER_ID)).thenReturn(List.of(r1, r2));

            List<SplitPaymentRule> result = splitPaymentService.getRulesByPartner(PARTNER_ID);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deactivateRule")
    class DeactivateRuleTests {

        @Test
        @DisplayName("should deactivate an active rule")
        void shouldDeactivateRule() {
            UUID ruleId = UUID.randomUUID();
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .id(ruleId).partnerId(PARTNER_ID).active(true)
                    .splitType(SplitType.PERCENTAGE).recipients(percentageRecipients)
                    .build();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));
            when(persistencePort.saveRule(any())).thenAnswer(inv -> inv.getArgument(0));

            splitPaymentService.deactivateRule(ruleId);

            ArgumentCaptor<SplitPaymentRule> captor = ArgumentCaptor.forClass(SplitPaymentRule.class);
            verify(persistencePort).saveRule(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Execution Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("executeSplit")
    class ExecuteSplitTests {

        private UUID ruleId;
        private SplitPaymentRule rule;

        @BeforeEach
        void setUp() {
            ruleId = UUID.randomUUID();
            rule = SplitPaymentRule.builder()
                    .id(ruleId)
                    .partnerId(PARTNER_ID)
                    .ruleName("Test Rule")
                    .splitType(SplitType.PERCENTAGE)
                    .currency(CURRENCY)
                    .active(true)
                    .recipients(percentageRecipients)
                    .build();
        }

        @Test
        @DisplayName("should execute split payment successfully — reserve, commit, credit each recipient")
        void shouldExecuteSplitSuccessfully() {
            String reservationId = "res-" + UUID.randomUUID();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));
            when(walletUseCase.reserveBalance(eq(PAYER_ACCOUNT), eq(TOTAL_AMOUNT), anyString()))
                    .thenReturn(reservationId);
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(persistencePort.saveExecution(any(SplitPaymentExecution.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentExecution result = splitPaymentService.executeSplit(
                    ruleId, PAYER_ACCOUNT, TOTAL_AMOUNT,
                    "order-ref-001", "Test split", "idem-key-001");

            // Verify state
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(SplitExecutionStatus.COMPLETED);
            assertThat(result.getPayerAccountId()).isEqualTo(PAYER_ACCOUNT);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
            assertThat(result.getLegs()).hasSize(3);
            assertThat(result.getCompletedAt()).isNotNull();

            // Verify all legs are CREDITED
            assertThat(result.getLegs()).allMatch(l -> l.getStatus() == LegStatus.CREDITED);

            // Verify wallet calls
            verify(walletUseCase).reserveBalance(eq(PAYER_ACCOUNT), eq(TOTAL_AMOUNT), anyString());
            verify(walletUseCase).commitReservation(reservationId);
            verify(walletUseCase, times(3)).credit(anyString(), any(BigDecimal.class), anyString(), anyString());

            // Verify journal was created with balanced entries (1 debit + 3 credits = 4)
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("Split payment"), eq("SPLIT_PAYMENT"), anyString(),
                    entriesCaptor.capture(), eq("split-payment-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries).hasSize(4);

            // First entry = debit payer
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.EntryType.DEBIT);
            assertThat(entries.get(0).getAccountId()).isEqualTo(PAYER_ACCOUNT);
            assertThat(entries.get(0).getAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

            // Remaining entries = credit recipients
            BigDecimal creditTotal = entries.subList(1, 4).stream()
                    .map(LedgerEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(creditTotal).isEqualByComparingTo(TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("should distribute amounts correctly: 70/25/5 split")
        void shouldDistributeAmountsCorrectly() {
            String reservationId = "res-" + UUID.randomUUID();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));
            when(walletUseCase.reserveBalance(anyString(), any(), anyString())).thenReturn(reservationId);
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(persistencePort.saveExecution(any())).thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentExecution result = splitPaymentService.executeSplit(
                    ruleId, PAYER_ACCOUNT, TOTAL_AMOUNT,
                    "order-ref", "Test", "idem-002");

            // 70% of 1,000,000 = 700,000
            // 25% of 1,000,000 = 250,000
            //  5% of 1,000,000 =  50,000 (last gets remainder)
            List<SplitPaymentLeg> legs = result.getLegs();
            BigDecimal legSum = legs.stream()
                    .map(SplitPaymentLeg::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(legSum).isEqualByComparingTo(TOTAL_AMOUNT);

            // Verify balanced
            assertThat(result.isBalanced()).isTrue();
        }

        @Test
        @DisplayName("should return existing execution for duplicate idempotency key")
        void shouldReturnExistingForDuplicateIdempotencyKey() {
            String idempotencyKey = "idem-duplicate-001";
            SplitPaymentExecution existing = SplitPaymentExecution.builder()
                    .id(UUID.randomUUID())
                    .payerAccountId(PAYER_ACCOUNT)
                    .totalAmount(TOTAL_AMOUNT)
                    .idempotencyKey(idempotencyKey)
                    .status(SplitExecutionStatus.COMPLETED)
                    .build();
            when(persistencePort.findExecutionByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existing));

            SplitPaymentExecution result = splitPaymentService.executeSplit(
                    ruleId, PAYER_ACCOUNT, TOTAL_AMOUNT,
                    "order-ref", "Test", idempotencyKey);

            assertThat(result.getId()).isEqualTo(existing.getId());
            assertThat(result.getStatus()).isEqualTo(SplitExecutionStatus.COMPLETED);

            // No wallet or journal calls should be made
            verifyNoInteractions(walletUseCase);
            verifyNoInteractions(journalUseCase);
        }

        @Test
        @DisplayName("should reject execution with inactive rule")
        void shouldRejectInactiveRule() {
            rule.setActive(false);
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));

            assertThatThrownBy(() -> splitPaymentService.executeSplit(
                    ruleId, PAYER_ACCOUNT, TOTAL_AMOUNT,
                    "order-ref", "Test", "idem-003"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("should handle wallet failure gracefully — release reservation and mark FAILED")
        void shouldHandleWalletFailureGracefully() {
            String reservationId = "res-" + UUID.randomUUID();
            when(persistencePort.findRuleById(ruleId)).thenReturn(Optional.of(rule));
            when(walletUseCase.reserveBalance(anyString(), any(), anyString())).thenReturn(reservationId);
            doThrow(new RuntimeException("Insufficient funds"))
                    .when(walletUseCase).commitReservation(reservationId);
            when(persistencePort.saveExecution(any())).thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentExecution result = splitPaymentService.executeSplit(
                    ruleId, PAYER_ACCOUNT, TOTAL_AMOUNT,
                    "order-ref", "Test", "idem-004");

            assertThat(result.getStatus()).isEqualTo(SplitExecutionStatus.FAILED);
            assertThat(result.getFailureReason()).contains("Insufficient funds");
            verify(walletUseCase).releaseReservation(reservationId);
        }
    }

    @Nested
    @DisplayName("executeAdHocSplit")
    class ExecuteAdHocSplitTests {

        @Test
        @DisplayName("should execute ad-hoc split with fixed amounts")
        void shouldExecuteAdHocSplitSuccessfully() {
            String reservationId = "res-" + UUID.randomUUID();
            when(walletUseCase.reserveBalance(anyString(), any(), anyString())).thenReturn(reservationId);
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(persistencePort.saveExecution(any())).thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentExecution result = splitPaymentService.executeAdHocSplit(
                    PAYER_ACCOUNT, PARTNER_ID, TOTAL_AMOUNT, CURRENCY,
                    fixedRecipients, "adhoc-ref", "Ad-hoc test", "idem-adhoc-001");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(SplitExecutionStatus.COMPLETED);
            assertThat(result.getSplitRuleId()).isNull(); // ad-hoc has no rule
            assertThat(result.getLegs()).hasSize(2);

            verify(walletUseCase).reserveBalance(eq(PAYER_ACCOUNT), eq(TOTAL_AMOUNT), anyString());
            verify(walletUseCase).commitReservation(reservationId);
            verify(walletUseCase, times(2)).credit(anyString(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("should return existing for duplicate idempotency key in ad-hoc")
        void shouldDeduplicateAdHocSplit() {
            String idempotencyKey = "idem-adhoc-dup";
            SplitPaymentExecution existing = SplitPaymentExecution.builder()
                    .id(UUID.randomUUID())
                    .status(SplitExecutionStatus.COMPLETED)
                    .idempotencyKey(idempotencyKey)
                    .build();
            when(persistencePort.findExecutionByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existing));

            SplitPaymentExecution result = splitPaymentService.executeAdHocSplit(
                    PAYER_ACCOUNT, PARTNER_ID, TOTAL_AMOUNT, CURRENCY,
                    fixedRecipients, "ref", "test", idempotencyKey);

            assertThat(result.getId()).isEqualTo(existing.getId());
            verifyNoInteractions(walletUseCase);
        }
    }

    @Nested
    @DisplayName("reverseExecution")
    class ReverseExecutionTests {

        @Test
        @DisplayName("should reverse a completed execution — credit payer, create reversal journal")
        void shouldReverseCompletedExecution() {
            UUID execId = UUID.randomUUID();
            SplitPaymentExecution execution = SplitPaymentExecution.builder()
                    .id(execId)
                    .payerAccountId(PAYER_ACCOUNT)
                    .totalAmount(TOTAL_AMOUNT)
                    .currency(CURRENCY)
                    .status(SplitExecutionStatus.COMPLETED)
                    .description("Original split")
                    .legs(Arrays.asList(
                            SplitPaymentLeg.builder()
                                    .id(UUID.randomUUID())
                                    .recipientAccountId(MERCHANT_A)
                                    .recipientLabel("Merchant A")
                                    .amount(new BigDecimal("700000"))
                                    .status(LegStatus.CREDITED)
                                    .build(),
                            SplitPaymentLeg.builder()
                                    .id(UUID.randomUUID())
                                    .recipientAccountId(PLATFORM_ACCOUNT)
                                    .recipientLabel("Platform Fee")
                                    .amount(new BigDecimal("300000"))
                                    .status(LegStatus.CREDITED)
                                    .build()
                    ))
                    .build();

            when(persistencePort.findExecutionById(execId)).thenReturn(Optional.of(execution));
            when(journalUseCase.createAndPostJournal(anyString(), anyString(), anyString(), anyList(), anyString()))
                    .thenReturn(JournalEntry.builder().id(UUID.randomUUID()).build());
            when(persistencePort.saveExecution(any())).thenAnswer(inv -> inv.getArgument(0));

            SplitPaymentExecution result = splitPaymentService.reverseExecution(execId, "Customer dispute");

            assertThat(result.getStatus()).isEqualTo(SplitExecutionStatus.REVERSED);

            // Payer should be credited total amount
            verify(walletUseCase).credit(eq(PAYER_ACCOUNT), eq(TOTAL_AMOUNT), anyString(), contains("reversal"));

            // All CREDITED legs should be REVERSED
            assertThat(result.getLegs()).allMatch(l -> l.getStatus() == LegStatus.REVERSED);

            // Reversal journal: 1 credit (payer) + 2 debits (recipients) = 3 entries
            ArgumentCaptor<List<LedgerEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);
            verify(journalUseCase).createAndPostJournal(
                    contains("reversal"), eq("SPLIT_PAYMENT"), anyString(),
                    entriesCaptor.capture(), eq("split-payment-service"));

            List<LedgerEntry> entries = entriesCaptor.getValue();
            assertThat(entries).hasSize(3);

            long credits = entries.stream().filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT).count();
            long debits = entries.stream().filter(e -> e.getEntryType() == LedgerEntry.EntryType.DEBIT).count();
            assertThat(credits).isEqualTo(1); // payer
            assertThat(debits).isEqualTo(2);  // recipients
        }

        @Test
        @DisplayName("should reject reversal for non-completed execution")
        void shouldRejectReversalForPendingExecution() {
            UUID execId = UUID.randomUUID();
            SplitPaymentExecution execution = SplitPaymentExecution.builder()
                    .id(execId)
                    .status(SplitExecutionStatus.PENDING)
                    .build();
            when(persistencePort.findExecutionById(execId)).thenReturn(Optional.of(execution));

            assertThatThrownBy(() -> splitPaymentService.reverseExecution(execId, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }
    }

    @Nested
    @DisplayName("getExecution & getExecutionsByPayer")
    class QueryExecutionTests {

        @Test
        @DisplayName("should return execution by ID")
        void shouldReturnExecutionById() {
            UUID execId = UUID.randomUUID();
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .id(execId).status(SplitExecutionStatus.COMPLETED).build();
            when(persistencePort.findExecutionById(execId)).thenReturn(Optional.of(exec));

            SplitPaymentExecution result = splitPaymentService.getExecution(execId);
            assertThat(result.getId()).isEqualTo(execId);
        }

        @Test
        @DisplayName("should throw when execution not found")
        void shouldThrowWhenExecutionNotFound() {
            UUID execId = UUID.randomUUID();
            when(persistencePort.findExecutionById(execId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> splitPaymentService.getExecution(execId))
                    .isInstanceOf(SplitPaymentNotFoundException.class);
        }

        @Test
        @DisplayName("should return executions by payer")
        void shouldReturnExecutionsByPayer() {
            SplitPaymentExecution e1 = SplitPaymentExecution.builder().id(UUID.randomUUID()).build();
            SplitPaymentExecution e2 = SplitPaymentExecution.builder().id(UUID.randomUUID()).build();
            when(persistencePort.findExecutionsByPayerAccountId(PAYER_ACCOUNT)).thenReturn(List.of(e1, e2));

            List<SplitPaymentExecution> result = splitPaymentService.getExecutionsByPayer(PAYER_ACCOUNT);
            assertThat(result).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Domain Model Tests
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("SplitPaymentRule Domain Model")
    class SplitPaymentRuleDomainTests {

        @Test
        @DisplayName("computeAmounts — percentage split with 3 recipients sums to total")
        void computeAmountsPercentageShouldSumToTotal() {
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .splitType(SplitType.PERCENTAGE)
                    .recipients(percentageRecipients)
                    .build();

            List<SplitPaymentRule.SplitLegAmount> legs = rule.computeAmounts(TOTAL_AMOUNT);

            assertThat(legs).hasSize(3);
            BigDecimal sum = legs.stream()
                    .map(l -> l.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("computeAmounts — handles rounding for odd amounts (e.g., 333.33 split 3 ways)")
        void computeAmountsShouldHandleRounding() {
            BigDecimal oddTotal = new BigDecimal("100");
            List<SplitRecipient> threeWay = Arrays.asList(
                    SplitRecipient.builder().recipientAccountId("a").recipientLabel("A")
                            .percentage(new BigDecimal("33.33")).priority(0).build(),
                    SplitRecipient.builder().recipientAccountId("b").recipientLabel("B")
                            .percentage(new BigDecimal("33.33")).priority(1).build(),
                    SplitRecipient.builder().recipientAccountId("c").recipientLabel("C")
                            .percentage(new BigDecimal("33.34")).priority(2).build()
            );

            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .splitType(SplitType.PERCENTAGE)
                    .recipients(threeWay)
                    .build();

            List<SplitPaymentRule.SplitLegAmount> legs = rule.computeAmounts(oddTotal);

            BigDecimal sum = legs.stream().map(l -> l.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Last recipient gets remainder, so sum always equals total
            assertThat(sum).isEqualByComparingTo(oddTotal);
        }

        @Test
        @DisplayName("computeAmounts — fixed split distributes exact amounts plus remainder")
        void computeAmountsFixedSplitShouldDistributeCorrectly() {
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .splitType(SplitType.FIXED)
                    .recipients(fixedRecipients)
                    .build();

            List<SplitPaymentRule.SplitLegAmount> legs = rule.computeAmounts(TOTAL_AMOUNT);

            assertThat(legs).hasSize(2);
            BigDecimal sum = legs.stream().map(l -> l.amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("validate — should throw for empty recipients")
        void validateShouldThrowForEmptyRecipients() {
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .splitType(SplitType.PERCENTAGE)
                    .recipients(List.of())
                    .build();

            assertThatThrownBy(rule::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least one recipient");
        }

        @Test
        @DisplayName("deactivate — should set active to false")
        void deactivateShouldSetActiveFalse() {
            SplitPaymentRule rule = SplitPaymentRule.builder()
                    .active(true).build();

            rule.deactivate();

            assertThat(rule.isActive()).isFalse();
            assertThat(rule.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("SplitPaymentExecution Domain Model")
    class SplitPaymentExecutionDomainTests {

        @Test
        @DisplayName("lifecycle: PENDING → PROCESSING → COMPLETED")
        void lifecycleHappyPath() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .id(UUID.randomUUID())
                    .status(SplitExecutionStatus.PENDING)
                    .build();

            exec.startProcessing();
            assertThat(exec.getStatus()).isEqualTo(SplitExecutionStatus.PROCESSING);

            exec.complete();
            assertThat(exec.getStatus()).isEqualTo(SplitExecutionStatus.COMPLETED);
            assertThat(exec.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("lifecycle: COMPLETED → REVERSED")
        void lifecycleReversal() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .id(UUID.randomUUID())
                    .status(SplitExecutionStatus.COMPLETED)
                    .build();

            exec.reverse();
            assertThat(exec.getStatus()).isEqualTo(SplitExecutionStatus.REVERSED);
        }

        @Test
        @DisplayName("should not complete from PENDING")
        void shouldNotCompleteFromPending() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .status(SplitExecutionStatus.PENDING).build();

            assertThatThrownBy(exec::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PROCESSING");
        }

        @Test
        @DisplayName("should not reverse from PENDING")
        void shouldNotReverseFromPending() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .status(SplitExecutionStatus.PENDING).build();

            assertThatThrownBy(exec::reverse)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("isBalanced — true when leg amounts sum to total")
        void isBalancedShouldBeTrue() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .totalAmount(new BigDecimal("1000"))
                    .legs(Arrays.asList(
                            SplitPaymentLeg.builder().amount(new BigDecimal("700")).build(),
                            SplitPaymentLeg.builder().amount(new BigDecimal("300")).build()
                    ))
                    .build();

            assertThat(exec.isBalanced()).isTrue();
        }

        @Test
        @DisplayName("isBalanced — false when legs don't sum to total")
        void isBalancedShouldBeFalse() {
            SplitPaymentExecution exec = SplitPaymentExecution.builder()
                    .totalAmount(new BigDecimal("1000"))
                    .legs(Arrays.asList(
                            SplitPaymentLeg.builder().amount(new BigDecimal("600")).build(),
                            SplitPaymentLeg.builder().amount(new BigDecimal("300")).build()
                    ))
                    .build();

            assertThat(exec.isBalanced()).isFalse();
        }
    }

    @Nested
    @DisplayName("SplitPaymentLeg Domain Model")
    class SplitPaymentLegDomainTests {

        @Test
        @DisplayName("markCredited — should set status and timestamp")
        void markCreditedShouldSetStatusAndTimestamp() {
            SplitPaymentLeg leg = SplitPaymentLeg.builder()
                    .id(UUID.randomUUID())
                    .status(LegStatus.PENDING)
                    .build();

            UUID journalId = UUID.randomUUID();
            leg.markCredited(journalId);

            assertThat(leg.getStatus()).isEqualTo(LegStatus.CREDITED);
            assertThat(leg.getJournalEntryId()).isEqualTo(journalId);
            assertThat(leg.getSettledAt()).isNotNull();
        }

        @Test
        @DisplayName("markReversed — should transition to REVERSED")
        void markReversedShouldTransition() {
            SplitPaymentLeg leg = SplitPaymentLeg.builder()
                    .status(LegStatus.CREDITED).build();

            leg.markReversed();
            assertThat(leg.getStatus()).isEqualTo(LegStatus.REVERSED);
        }

        @Test
        @DisplayName("markFailed — should transition to FAILED")
        void markFailedShouldTransition() {
            SplitPaymentLeg leg = SplitPaymentLeg.builder()
                    .status(LegStatus.PENDING).build();

            leg.markFailed();
            assertThat(leg.getStatus()).isEqualTo(LegStatus.FAILED);
        }
    }
}
