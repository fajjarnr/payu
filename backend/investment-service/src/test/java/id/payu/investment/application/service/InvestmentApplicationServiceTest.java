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
}
