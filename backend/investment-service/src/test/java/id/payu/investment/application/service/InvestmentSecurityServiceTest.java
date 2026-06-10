package id.payu.investment.application.service;

import id.payu.investment.domain.model.AccountStatus;
import id.payu.investment.domain.model.InvestmentAccount;
import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentSecurityService")
class InvestmentSecurityServiceTest {

    @Mock
    private InvestmentPersistencePort investmentPersistencePort;

    @InjectMocks
    private InvestmentSecurityService securityService;

    @Nested
    @DisplayName("isAccountOwner")
    class IsAccountOwner {

        @Test
        @DisplayName("should return true when user is the account owner")
        void shouldReturnTrueWhenUserIsOwner() {
            String accountId = UUID.randomUUID().toString();
            String userId = "user-123";

            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.fromString(accountId))
                    .userId(userId)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .status(AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(accountId)))
                    .willReturn(Optional.of(account));

            assertThat(securityService.isAccountOwner(accountId, userId)).isTrue();
        }

        @Test
        @DisplayName("should return false when user is not the account owner")
        void shouldReturnFalseWhenUserIsNotOwner() {
            String accountId = UUID.randomUUID().toString();

            InvestmentAccount account = InvestmentAccount.builder()
                    .id(UUID.fromString(accountId))
                    .userId("other-user")
                    .totalBalance(BigDecimal.ZERO)
                    .status(AccountStatus.ACTIVE)
                    .build();

            given(investmentPersistencePort.findAccountById(UUID.fromString(accountId)))
                    .willReturn(Optional.of(account));

            assertThat(securityService.isAccountOwner(accountId, "user-123")).isFalse();
        }

        @Test
        @DisplayName("should return false when account is not found")
        void shouldReturnFalseWhenAccountNotFound() {
            String accountId = UUID.randomUUID().toString();

            given(investmentPersistencePort.findAccountById(UUID.fromString(accountId)))
                    .willReturn(Optional.empty());

            assertThat(securityService.isAccountOwner(accountId, "user-123")).isFalse();
        }

        @Test
        @DisplayName("should return false when accountId is null")
        void shouldReturnFalseWhenAccountIdIsNull() {
            assertThat(securityService.isAccountOwner(null, "user-123")).isFalse();
        }

        @Test
        @DisplayName("should return false when userId is null")
        void shouldReturnFalseWhenUserIdIsNull() {
            assertThat(securityService.isAccountOwner(UUID.randomUUID().toString(), null)).isFalse();
        }

        @Test
        @DisplayName("should return false for invalid UUID format")
        void shouldReturnFalseForInvalidUuid() {
            assertThat(securityService.isAccountOwner("not-a-uuid", "user-123")).isFalse();
        }
    }
}
