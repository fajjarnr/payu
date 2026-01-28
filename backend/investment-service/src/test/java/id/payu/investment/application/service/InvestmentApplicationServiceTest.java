package id.payu.investment.application.service;

import id.payu.investment.domain.model.*;
import id.payu.investment.domain.port.out.InvestmentEventPublisherPort;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import id.payu.investment.domain.port.out.WalletServicePort;
import id.payu.investment.dto.InvestmentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentApplicationService")
class InvestmentApplicationServiceTest {

    @Mock
    private InvestmentPersistencePort investmentPersistencePort;

    @Mock
    private WalletServicePort walletServicePort;

    @Mock
    private InvestmentEventPublisherPort investmentEventPublisherPort;

    @InjectMocks
    private InvestmentApplicationService investmentApplicationService;

    private String testUserId;
    private String testAccountId;
    private BigDecimal testAmount;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testAccountId = UUID.randomUUID().toString();
        testAmount = new BigDecimal("1000000.00");
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("should create account successfully when user doesn't have one")
        void shouldCreateAccountSuccessfully() throws ExecutionException, InterruptedException {
            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.existsAccountByUserId(testUserId)).willReturn(false);
            given(investmentPersistencePort.saveAccount(any(InvestmentAccount.class))).willReturn(account);

            CompletableFuture<InvestmentAccount> result = investmentApplicationService.createAccount(testUserId);
            InvestmentAccount createdAccount = result.get();

            assertThat(createdAccount).isNotNull();
            assertThat(createdAccount.getUserId()).isEqualTo(testUserId);
            verify(investmentPersistencePort).saveAccount(any(InvestmentAccount.class));
            verify(investmentEventPublisherPort).publishInvestmentCreated(any(InvestmentEvent.class));
        }

        @Test
        @DisplayName("should throw exception when account already exists")
        void shouldThrowExceptionWhenAccountExists() {
            given(investmentPersistencePort.existsAccountByUserId(testUserId)).willReturn(true);

            assertThatThrownBy(() -> investmentApplicationService.createAccount(testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Investment account already exists");

            verify(investmentPersistencePort, never()).saveAccount(any(InvestmentAccount.class));
        }
    }

    @Nested
    @DisplayName("buyGold")
    class BuyGold {

        @Test
        @DisplayName("should buy gold successfully when balance is sufficient")
        void shouldBuyGoldSuccessfully() throws ExecutionException, InterruptedException {
            given(investmentPersistencePort.getLatestGoldPrice()).willReturn(new BigDecimal("1250000.00"));
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(true);
            given(investmentPersistencePort.findGoldByUserId(testUserId)).willReturn(Optional.empty());

            Gold gold = Gold.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .amount(new BigDecimal("0.8000"))
                    .averageBuyPrice(new BigDecimal("1250000.00"))
                    .currentPrice(new BigDecimal("1250000.00"))
                    .currentValue(testAmount)
                    .build();

            given(investmentPersistencePort.saveGold(any(Gold.class))).willReturn(gold);

            CompletableFuture<Gold> result = investmentApplicationService.buyGold(testUserId, testAmount);
            Gold boughtGold = result.get();

            assertThat(boughtGold).isNotNull();
            assertThat(boughtGold.getUserId()).isEqualTo(testUserId);
            verify(walletServicePort).deductBalance(testUserId, testAmount);
            verify(investmentPersistencePort).saveGold(any(Gold.class));
            verify(investmentEventPublisherPort).publishInvestmentCompleted(any(InvestmentEvent.class));
        }
    }

    @Nested
    @DisplayName("buyMutualFund")
    class BuyMutualFund {

        @Test
        @DisplayName("should buy mutual fund successfully when balance is sufficient")
        void shouldBuyMutualFundSuccessfully() throws ExecutionException, InterruptedException {
            String fundCode = "MMF001";
            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId))).willReturn(Optional.of(account));

            MutualFund fund = MutualFund.builder()
                    .id(UUID.randomUUID())
                    .code(fundCode)
                    .name("PayU Money Market Fund")
                    .type(MutualFund.FundType.MONEY_MARKET)
                    .navPerUnit(new BigDecimal("1500.0000"))
                    .minimumInvestment(new BigDecimal("10000.0000"))
                    .managementFee(new BigDecimal("0.0050"))
                    .redemptionFee(new BigDecimal("0.0020"))
                    .status(MutualFund.FundStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.getLatestFundPrice(fundCode)).willReturn(fund);
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(true);

            InvestmentTransaction transaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .type(InvestmentTransaction.TransactionType.BUY)
                    .investmentType(InvestmentTransaction.InvestmentType.MUTUAL_FUND)
                    .investmentId(fundCode)
                    .amount(testAmount)
                    .price(new BigDecimal("1500.0000"))
                    .units(new BigDecimal("666.6666"))
                    .fee(new BigDecimal("2000.0000"))
                    .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                    .build();

            given(investmentPersistencePort.saveTransaction(any(InvestmentTransaction.class))).willReturn(transaction);

            CompletableFuture<InvestmentTransaction> result = investmentApplicationService.buyMutualFund(
                    testAccountId, testUserId, fundCode, testAmount);
            InvestmentTransaction boughtTransaction = result.get();

            assertThat(boughtTransaction).isNotNull();
            assertThat(boughtTransaction.getInvestmentId()).isEqualTo(fundCode);
            verify(walletServicePort).deductBalance(testUserId, testAmount);
            verify(investmentPersistencePort).saveTransaction(any(InvestmentTransaction.class));
            verify(investmentEventPublisherPort).publishInvestmentCompleted(any(InvestmentEvent.class));
        }

        @Test
        @DisplayName("should throw exception when amount is below minimum investment")
        void shouldThrowExceptionWhenAmountBelowMinimum() {
            String fundCode = "MMF001";
            BigDecimal smallAmount = new BigDecimal("5000.00");

            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId))).willReturn(Optional.of(account));

            MutualFund fund = MutualFund.builder()
                    .id(UUID.randomUUID())
                    .code(fundCode)
                    .name("PayU Money Market Fund")
                    .type(MutualFund.FundType.MONEY_MARKET)
                    .navPerUnit(new BigDecimal("1500.0000"))
                    .minimumInvestment(new BigDecimal("10000.0000"))
                    .managementFee(new BigDecimal("0.0050"))
                    .redemptionFee(new BigDecimal("0.0020"))
                    .status(MutualFund.FundStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.getLatestFundPrice(fundCode)).willReturn(fund);

            assertThatThrownBy(() -> investmentApplicationService.buyMutualFund(
                    testAccountId, testUserId, fundCode, smallAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount below minimum investment");

            verify(walletServicePort, never()).deductBalance(anyString(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("sellInvestment")
    class SellInvestment {

        @Test
        @DisplayName("should sell mutual fund successfully")
        void shouldSellMutualFundSuccessfully() throws ExecutionException, InterruptedException {
            String fundCode = "MMF001";
            UUID transactionId = UUID.randomUUID();

            InvestmentTransaction existingTransaction = InvestmentTransaction.builder()
                    .id(transactionId)
                    .accountId(testAccountId)
                    .type(InvestmentTransaction.TransactionType.BUY)
                    .investmentType(InvestmentTransaction.InvestmentType.MUTUAL_FUND)
                    .investmentId(fundCode)
                    .amount(testAmount)
                    .price(new BigDecimal("1500.0000"))
                    .units(new BigDecimal("666.6666"))
                    .fee(new BigDecimal("2000.0000"))
                    .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                    .build();

            given(investmentPersistencePort.findTransactionById(transactionId)).willReturn(Optional.of(existingTransaction));

            MutualFund fund = MutualFund.builder()
                    .id(UUID.randomUUID())
                    .code(fundCode)
                    .name("PayU Money Market Fund")
                    .type(MutualFund.FundType.MONEY_MARKET)
                    .navPerUnit(new BigDecimal("1600.0000"))
                    .minimumInvestment(new BigDecimal("10000.0000"))
                    .managementFee(new BigDecimal("0.0050"))
                    .redemptionFee(new BigDecimal("0.0020"))
                    .status(MutualFund.FundStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.getLatestFundPrice(fundCode)).willReturn(fund);

            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId))).willReturn(Optional.of(account));

            InvestmentTransaction sellTransaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .type(InvestmentTransaction.TransactionType.SELL)
                    .investmentType(InvestmentTransaction.InvestmentType.MUTUAL_FUND)
                    .investmentId(fundCode)
                    .amount(new BigDecimal("800000.00"))
                    .price(new BigDecimal("1600.0000"))
                    .units(new BigDecimal("500.0000"))
                    .fee(new BigDecimal("4000.0000"))
                    .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                    .build();

            given(investmentPersistencePort.saveTransaction(any(InvestmentTransaction.class))).willReturn(sellTransaction);

            CompletableFuture<InvestmentTransaction> result = investmentApplicationService.sellInvestment(
                    testAccountId, transactionId, new BigDecimal("500.0000"));
            InvestmentTransaction soldTransaction = result.get();

            assertThat(soldTransaction).isNotNull();
            assertThat(soldTransaction.getType()).isEqualTo(InvestmentTransaction.TransactionType.SELL);
            verify(walletServicePort).creditBalance(eq(testUserId), any(BigDecimal.class));
            verify(investmentPersistencePort).saveTransaction(any(InvestmentTransaction.class));
            verify(investmentEventPublisherPort).publishInvestmentCompleted(any(InvestmentEvent.class));
        }

        @Test
        @DisplayName("should throw exception when trying to sell deposit before maturity")
        void shouldThrowExceptionWhenSellingDeposit() {
            UUID transactionId = UUID.randomUUID();

            InvestmentTransaction existingTransaction = InvestmentTransaction.builder()
                    .id(transactionId)
                    .accountId(testAccountId)
                    .type(InvestmentTransaction.TransactionType.BUY)
                    .investmentType(InvestmentTransaction.InvestmentType.DEPOSIT)
                    .investmentId(UUID.randomUUID().toString())
                    .amount(testAmount)
                    .status(InvestmentTransaction.TransactionStatus.COMPLETED)
                    .build();

            given(investmentPersistencePort.findTransactionById(transactionId)).willReturn(Optional.of(existingTransaction));

            assertThatThrownBy(() -> investmentApplicationService.sellInvestment(
                    testAccountId, transactionId, testAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot sell deposit before maturity");

            verify(walletServicePort, never()).creditBalance(anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should throw exception when selling non-existent transaction")
        void shouldThrowExceptionWhenSellingNonExistentTransaction() {
            UUID transactionId = UUID.randomUUID();

            given(investmentPersistencePort.findTransactionById(transactionId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> investmentApplicationService.sellInvestment(
                    testAccountId, transactionId, testAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction not found");

            verify(walletServicePort, never()).creditBalance(anyString(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("buyDeposit")
    class BuyDeposit {

        @Test
        @DisplayName("should buy deposit successfully")
        void shouldBuyDepositSuccessfully() throws ExecutionException, InterruptedException {
            int tenure = 6;

            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId))).willReturn(Optional.of(account));
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(true);

            Deposit deposit = Deposit.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .amount(testAmount)
                    .tenure(tenure)
                    .interestRate(new BigDecimal("0.0550"))
                    .maturityAmount(new BigDecimal("1027500.00"))
                    .status(Deposit.DepositStatus.ACTIVE)
                    .currency("IDR")
                    .build();

            given(investmentPersistencePort.saveDeposit(any(Deposit.class))).willReturn(deposit);

            CompletableFuture<Deposit> result = investmentApplicationService.buyDeposit(
                    testAccountId, testUserId, testAmount, tenure);
            Deposit boughtDeposit = result.get();

            assertThat(boughtDeposit).isNotNull();
            assertThat(boughtDeposit.getTenure()).isEqualTo(tenure);
            verify(walletServicePort).deductBalance(testUserId, testAmount);
            verify(investmentPersistencePort).saveDeposit(any(Deposit.class));
            verify(investmentPersistencePort).saveTransaction(any(InvestmentTransaction.class));
            verify(investmentEventPublisherPort).publishInvestmentCompleted(any(InvestmentEvent.class));
        }

        @Test
        @DisplayName("should throw exception when buying deposit with insufficient balance")
        void shouldThrowExceptionWhenInsufficientBalance() {
            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId)))
                    .willReturn(Optional.of(InvestmentAccount.builder()
                            .id(UUID.randomUUID())
                            .userId(testUserId)
                            .build()));
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(false);

            assertThatThrownBy(() -> investmentApplicationService.buyDeposit(
                    testAccountId, testUserId, testAmount, 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient wallet balance");

            verify(walletServicePort, never()).deductBalance(anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            given(investmentPersistencePort.findAccountById(UUID.fromString(testAccountId))).willReturn(Optional.empty());

            assertThatThrownBy(() -> investmentApplicationService.buyDeposit(
                    testAccountId, testUserId, testAmount, 6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Investment account not found");
        }
    }

    @Nested
    @DisplayName("getAccountByUserId")
    class GetAccountByUserId {

        @Test
        @DisplayName("should get account by user id successfully")
        void shouldGetAccountByUserId() throws ExecutionException, InterruptedException {
            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(new BigDecimal("5000000"))
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountByUserId(testUserId)).willReturn(Optional.of(account));

            CompletableFuture<InvestmentAccount> result = investmentApplicationService.getAccountByUserId(testUserId);
            InvestmentAccount foundAccount = result.get();

            assertThat(foundAccount).isNotNull();
            assertThat(foundAccount.getUserId()).isEqualTo(testUserId);
            verify(investmentPersistencePort).findAccountByUserId(testUserId);
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFoundByUserId() {
            given(investmentPersistencePort.findAccountByUserId(testUserId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> investmentApplicationService.getAccountByUserId(testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Investment account not found");
        }
    }

    @Nested
    @DisplayName("getGoldByUserId")
    class GetGoldByUserId {

        @Test
        @DisplayName("should get gold by user id successfully")
        void shouldGetGoldByUserId() throws ExecutionException, InterruptedException {
            Gold gold = Gold.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .amount(new BigDecimal("10.5"))
                    .averageBuyPrice(new BigDecimal("1200000"))
                    .currentValue(new BigDecimal("12600000"))
                    .build();

            given(investmentPersistencePort.findGoldByUserId(testUserId)).willReturn(Optional.of(gold));

            CompletableFuture<Gold> result = investmentApplicationService.getGoldByUserId(testUserId);
            Gold foundGold = result.get();

            assertThat(foundGold).isNotNull();
            assertThat(foundGold.getUserId()).isEqualTo(testUserId);
            verify(investmentPersistencePort).findGoldByUserId(testUserId);
        }

        @Test
        @DisplayName("should throw exception when gold not found")
        void shouldThrowExceptionWhenGoldNotFound() {
            given(investmentPersistencePort.findGoldByUserId(testUserId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> investmentApplicationService.getGoldByUserId(testUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gold holdings not found");
        }
    }

    @Nested
    @DisplayName("buyGold error scenarios")
    class BuyGoldErrors {

        @Test
        @DisplayName("should throw exception when gold price not available")
        void shouldThrowExceptionWhenGoldPriceNotAvailable() {
            given(investmentPersistencePort.getLatestGoldPrice()).willReturn(null);

            assertThatThrownBy(() -> investmentApplicationService.buyGold(testUserId, testAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gold price not available");

            verify(walletServicePort, never()).deductBalance(anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should throw exception when buying gold with insufficient balance")
        void shouldThrowExceptionWhenBuyingGoldWithInsufficientBalance() {
            given(investmentPersistencePort.getLatestGoldPrice()).willReturn(new BigDecimal("1250000"));
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(false);

            assertThatThrownBy(() -> investmentApplicationService.buyGold(testUserId, testAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient wallet balance");

            verify(walletServicePort, never()).deductBalance(anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should add to existing gold holdings")
        void shouldAddToExistingGoldHoldings() throws ExecutionException, InterruptedException {
            BigDecimal goldPrice = new BigDecimal("1250000.00");

            Gold existingGold = Gold.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .amount(new BigDecimal("1.0000"))
                    .averageBuyPrice(new BigDecimal("1200000.00"))
                    .currentPrice(goldPrice)
                    .currentValue(new BigDecimal("1250000.00"))
                    .unrealizedProfitLoss(new BigDecimal("50000.00"))
                    .build();

            given(investmentPersistencePort.getLatestGoldPrice()).willReturn(goldPrice);
            given(walletServicePort.hasSufficientBalance(testUserId, testAmount)).willReturn(true);
            given(investmentPersistencePort.findGoldByUserId(testUserId)).willReturn(Optional.of(existingGold));

            Gold updatedGold = Gold.builder()
                    .id(existingGold.getId())
                    .userId(testUserId)
                    .amount(new BigDecimal("1.8000"))
                    .averageBuyPrice(new BigDecimal("1222222.22"))
                    .currentPrice(goldPrice)
                    .currentValue(new BigDecimal("2250000.00"))
                    .build();

            given(investmentPersistencePort.saveGold(any(Gold.class))).willReturn(updatedGold);

            CompletableFuture<Gold> result = investmentApplicationService.buyGold(testUserId, testAmount);
            Gold boughtGold = result.get();

            assertThat(boughtGold).isNotNull();
            verify(walletServicePort).deductBalance(testUserId, testAmount);
            verify(investmentPersistencePort).saveGold(any(Gold.class));
        }
    }
}
