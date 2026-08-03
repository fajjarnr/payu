package id.payu.investment.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.investment.application.service.InvestmentApplicationService;
import id.payu.investment.domain.model.AccountStatus;
import id.payu.investment.domain.model.Deposit;
import id.payu.investment.domain.model.DepositStatus;
import id.payu.investment.domain.model.Gold;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.model.InvestmentTransaction;
import id.payu.investment.domain.model.InvestmentType;
import id.payu.investment.domain.model.TransactionStatus;
import id.payu.investment.domain.model.TransactionType;
import id.payu.investment.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testAccountId = UUID.randomUUID().toString();
        testTransactionId = UUID.randomUUID();
        testAmount = new BigDecimal("1000000.00");
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getSubject()).thenReturn(testUserId);
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
                    .status(AccountStatus.ACTIVE)
                    .build();

            given(investmentApplicationService.createAccount(testUserId))
                    .willReturn(CompletableFuture.completedFuture(account));

            ResponseEntity<ApiResponse<InvestmentAccount>> response =
                    investmentController.createAccount(mockJwt)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getUserId()).isEqualTo(testUserId);
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
                    .status(DepositStatus.ACTIVE)
                    .build();

            BuyDepositRequest request = new BuyDepositRequest(testAccountId, testAmount, 6);

            given(investmentApplicationService.buyDeposit(testAccountId, testUserId, testAmount, 6, "idem-deposit"))
                    .willReturn(CompletableFuture.completedFuture(deposit));

            ResponseEntity<ApiResponse<Deposit>> response =
                    investmentController.buyDeposit(request, "idem-deposit", mockJwt)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getAmount()).isEqualTo(testAmount);
            verify(investmentApplicationService).buyDeposit(testAccountId, testUserId, testAmount, 6, "idem-deposit");
        }
    }

    @Nested
    @DisplayName("buyMutualFund")
    class BuyMutualFund {

        @Test
        @DisplayName("should buy mutual fund successfully")
        void shouldBuyMutualFundSuccessfully() throws Exception {
            String fundCode = "FUND001";
            InvestmentTransaction transaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .type(TransactionType.BUY)
                    .investmentType(InvestmentType.MUTUAL_FUND)
                    .investmentId(fundCode)
                    .amount(testAmount)
                    .status(TransactionStatus.COMPLETED)
                    .build();

            BuyMutualFundRequest request = new BuyMutualFundRequest(testAccountId, fundCode, testAmount);

            given(investmentApplicationService.buyMutualFund(testAccountId, testUserId, fundCode, testAmount, "idem-fund"))
                    .willReturn(CompletableFuture.completedFuture(transaction));

            ResponseEntity<ApiResponse<InvestmentTransaction>> response =
                    investmentController.buyMutualFund(request, "idem-fund", mockJwt)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getAmount()).isEqualTo(testAmount);
            verify(investmentApplicationService).buyMutualFund(testAccountId, testUserId, fundCode, testAmount, "idem-fund");
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

            BuyGoldRequest request = new BuyGoldRequest(testAmount);

            given(investmentApplicationService.buyGold(testUserId, testAmount, "idem-gold"))
                    .willReturn(CompletableFuture.completedFuture(gold));

            ResponseEntity<ApiResponse<Gold>> response =
                    investmentController.buyGold(request, "idem-gold", mockJwt)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getUserId()).isEqualTo(testUserId);
            verify(investmentApplicationService).buyGold(testUserId, testAmount, "idem-gold");
        }
    }

    @Nested
    @DisplayName("sellInvestment")
    class SellInvestment {

        @Test
        @DisplayName("should sell investment successfully")
        void shouldSellInvestmentSuccessfully() throws Exception {
            InvestmentTransaction transaction = InvestmentTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(testAccountId)
                    .type(TransactionType.SELL)
                    .investmentType(InvestmentType.MUTUAL_FUND)
                    .amount(testAmount)
                    .status(TransactionStatus.COMPLETED)
                    .build();

            SellInvestmentRequest request = new SellInvestmentRequest(testAccountId, testTransactionId, testAmount);

            given(investmentApplicationService.sellInvestment(testAccountId, testTransactionId, testAmount))
                    .willReturn(CompletableFuture.completedFuture(transaction));

            ResponseEntity<ApiResponse<InvestmentTransaction>> response =
                    investmentController.sellInvestment(request, mockJwt)
                            .get();

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getAmount()).isEqualTo(testAmount);
            verify(investmentApplicationService).sellInvestment(testAccountId, testTransactionId, testAmount);
        }
    }
}
