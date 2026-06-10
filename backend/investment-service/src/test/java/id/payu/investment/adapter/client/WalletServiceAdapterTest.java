package id.payu.investment.adapter.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletServiceAdapter")
class WalletServiceAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WalletServiceAdapter walletServiceAdapter;

    private String userId;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        amount = new BigDecimal("1000000.00");
    }

    @Nested
    @DisplayName("hasSufficientBalance")
    class HasSufficientBalance {

        @Test
        @DisplayName("should return true when availableBalance is sufficient")
        void shouldReturnTrueWhenBalanceSufficient() {
            Map<String, Object> response = Map.of("availableBalance", "2000000.00", "balance", "2500000.00");
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

            boolean result = walletServiceAdapter.hasSufficientBalance(userId, amount);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when availableBalance is insufficient")
        void shouldReturnFalseWhenBalanceInsufficient() {
            Map<String, Object> response = Map.of("availableBalance", "500000.00");
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

            boolean result = walletServiceAdapter.hasSufficientBalance(userId, amount);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should unwrap ApiResponse wrapper for availableBalance")
        void shouldUnwrapApiResponse() {
            Map<String, Object> data = Map.of("availableBalance", "3000000.00");
            Map<String, Object> response = Map.of("data", data);
            given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

            boolean result = walletServiceAdapter.hasSufficientBalance(userId, amount);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false on RestTemplate exception")
        void shouldReturnFalseOnRestException() {
            given(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .willThrow(new RuntimeException("Connection refused"));

            boolean result = walletServiceAdapter.hasSufficientBalance(userId, amount);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("creditBalance")
    class CreditBalance {

        @Test
        @DisplayName("should post to credit endpoint with idempotency key")
        void shouldCreditWithIdempotencyKey() {
            given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .willReturn(Map.of("status", "SUCCESS"));

            walletServiceAdapter.creditBalance(userId, amount);

            ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(contains("/credit"), captor.capture(), eq(Map.class));

            HttpEntity<Map<String, Object>> entity = captor.getValue();
            assertThat(entity.getHeaders().getFirst("X-Idempotency-Key")).isNotNull();
        }

        @Test
        @DisplayName("should throw on credit failure")
        void shouldThrowOnCreditFailure() {
            given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                    .willThrow(new RuntimeException("Service unavailable"));

            assertThatThrownBy(() -> walletServiceAdapter.creditBalance(userId, amount))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to credit wallet balance");
        }
    }

    @Nested
    @DisplayName("deductBalance")
    class DeductBalance {

        @Test
        @DisplayName("should reserve and commit balance deduction")
        void shouldReserveAndCommitBalance() {
            Map<String, Object> reserveResponse = Map.of("reservationId", "res-001");
            given(restTemplate.postForObject(contains("/reserve"), any(HttpEntity.class), eq(Map.class)))
                    .willReturn(reserveResponse);
            given(restTemplate.postForObject(contains("/commit"), isNull(), eq(Map.class)))
                    .willReturn(Map.of());

            walletServiceAdapter.deductBalance(userId, amount);

            verify(restTemplate).postForObject(contains("/reserve"), any(HttpEntity.class), eq(Map.class));
            verify(restTemplate).postForObject(contains("/res-001/commit"), isNull(), eq(Map.class));
        }

        @Test
        @DisplayName("should throw when reserve returns null")
        void shouldThrowWhenReserveReturnsNull() {
            given(restTemplate.postForObject(contains("/reserve"), any(HttpEntity.class), eq(Map.class)))
                    .willReturn(null);

            assertThatThrownBy(() -> walletServiceAdapter.deductBalance(userId, amount))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid reserve response");
        }
    }
}
