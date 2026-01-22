package id.payu.investment.adapter.web;

import id.payu.investment.application.service.InvestmentApplicationService;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentController")
class InvestmentControllerTest {

    @Mock
    private InvestmentApplicationService investmentApplicationService;

    @InjectMocks
    private InvestmentController investmentController;

    private String testUserId;
    private String testAccountId;
    private UUID testTransactionId;
    private BigDecimal testAmount;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testAccountId = UUID.randomUUID().toString();
        testTransactionId = UUID.randomUUID();
        testAmount = new BigDecimal("1000000.00");
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("should create investment account successfully")
        void shouldCreateAccountSuccessfully() throws Exception {
            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(InvestmentAccount.AccountStatus.ACTIVE)
                    .build();

            given(investmentApplicationService.createAccount(testUserId))
                    .willReturn(CompletableFuture.completedFuture(account));

            ResponseEntity<InvestmentAccount> response = 
                    investmentController.createAccount(new id.payu.investment.dto.CreateInvestmentAccountRequest(testUserId))
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
            verify(investmentApplicationService).createAccount(testUserId);
        }
    }

    @Nested
    @DisplayName("buyDeposit")
    class BuyDeposit {

        @Test
        @DisplayName("should buy deposit successfully")
        void shouldBuyDepositSuccessfully() throws Exception {
            Deposit deposit = Deposit.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .amount(testAmount)
                    .tenure(6)
                    .status(Deposit.DepositStatus.ACTIVE)
                    .build();

            given(investmentApplicationService.buyDeposit(testAccountId, testUserId, testAmount, 6))
                    .willReturn(CompletableFuture.completedFuture(deposit));

            ResponseEntity<Deposit> response = 
                    investmentController.buyDeposit(testAccountId, testUserId, testAmount, 6)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAmount()).isEqualTo(testAmount);
            verify(investmentApplicationService).buyDeposit(testAccountId, testUserId, testAmount, 6);
        }
    }

    @Nested
    @DisplayName("buyGold")
    class BuyGold {

        @Test
        @DisplayName("should buy gold successfully")
        void shouldBuyGoldSuccessfully() throws Exception {
            Gold gold = Gold.builder()
                    .id(UUID.randomUUID())
                    .userId(testUserId)
                    .amount(new BigDecimal("0.8000"))
                    .averageBuyPrice(new BigDecimal("1250000.00"))
                    .currentValue(testAmount)
                    .build();

            given(investmentApplicationService.buyGold(testUserId, testAmount))
                    .willReturn(CompletableFuture.completedFuture(gold));

            ResponseEntity<Gold> response = 
                    investmentController.buyGold(testUserId, testAmount)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
            verify(investmentApplicationService).buyGold(testUserId, testAmount);
        }
    }
}
