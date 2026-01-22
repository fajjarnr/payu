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
    }
}
