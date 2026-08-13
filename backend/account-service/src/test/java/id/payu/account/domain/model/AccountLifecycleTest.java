package id.payu.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ACCOUNT-006: core-domain lifecycle coverage for {@link Account} — status
 * transitions, validation guards, ownership and balance queries.
 */
@DisplayName("Account lifecycle domain")
class AccountLifecycleTest {

    private Account activeAccount(String type) {
        return new Account(UUID.randomUUID(), "ext-1", UUID.randomUUID(), "1234567890",
                type, AccountStatus.ACTIVE, new BigDecimal("1000.0000"),
                "IDR", LocalDateTime.now(), LocalDateTime.now(), 0);
    }

    @Test
    void statusTransitions() {
        Account a = new Account(UUID.randomUUID(), "ext-1", UUID.randomUUID(), "1234567890",
                "SAVINGS", AccountStatus.PENDING_VERIFICATION, BigDecimal.ZERO,
                "IDR", LocalDateTime.now(), LocalDateTime.now(), 0);

        assertThat(a.isPendingVerification()).isTrue();
        assertThat(a.isActive()).isFalse();

        a.activate();
        assertThat(a.isActive()).isTrue();
        assertThat(a.isPendingVerification()).isFalse();

        a.freeze();
        assertThat(a.isFrozen()).isTrue();

        a.unfreeze();
        assertThat(a.isActive()).isTrue();

        a.close();
        assertThat(a.isClosed()).isTrue();
    }

    @Test
    void invalidTransitionsThrow() {
        Account a = activeAccount("SAVINGS");
        assertThatThrownBy(a::activate).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(a::unfreeze).isInstanceOf(IllegalStateException.class);

        a.freeze();
        assertThatThrownBy(a::freeze).isInstanceOf(IllegalStateException.class);

        Account b = activeAccount("SAVINGS");
        b.credit(new BigDecimal("1.0000"));
        assertThatThrownBy(b::close).isInstanceOf(IllegalStateException.class);

        Account c = new Account(UUID.randomUUID(), "ext-1", UUID.randomUUID(), "1234567890",
                "SAVINGS", AccountStatus.CLOSED, BigDecimal.ZERO, "IDR",
                LocalDateTime.now(), LocalDateTime.now(), 0);
        assertThatThrownBy(c::close).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void debitValidationGuards() {
        Account a = activeAccount("SAVINGS");
        assertThatThrownBy(() -> a.debit(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.debit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.debit(new BigDecimal("9999.0000")))
                .isInstanceOf(Account.InsufficientFundsException.class);
        assertThatThrownBy(() -> a.debit(new BigDecimal("900.0000")))
                .as("debit below minimum savings balance must be rejected")
                .isInstanceOf(Account.InsufficientFundsException.class);
    }

    @Test
    void creditValidationGuards() {
        Account a = activeAccount("SAVINGS");
        assertThatThrownBy(() -> a.credit(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.credit(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        a.credit(new BigDecimal("499.0000"));
        assertThat(a.getBalance()).isEqualByComparingTo(new BigDecimal("1499.0000"));
    }

    @Test
    void frozenAccountRejectsMoneyMovement() {
        Account a = activeAccount("SAVINGS");
        a.freeze();
        assertThatThrownBy(() -> a.credit(BigDecimal.ONE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> a.debit(BigDecimal.ONE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void queries() {
        Account a = activeAccount("CHECKING");
        UUID owner = a.getUserId();
        assertThat(a.isOwnedBy(owner)).isTrue();
        assertThat(a.isOwnedBy(UUID.randomUUID())).isFalse();
        assertThat(a.hasSufficientFunds(new BigDecimal("1000.0000"))).isTrue();
        assertThat(a.hasSufficientFunds(new BigDecimal("1000.0001"))).isFalse();
        assertThat(a.canMaintainMinimumBalance(new BigDecimal("900.0000")))
                .as("balance 1000 minus 900 keeps above checking minimum 50000? no")
                .isFalse();
        assertThat(activeAccount("POCKET").canMaintainMinimumBalance(new BigDecimal("999.9999")))
                .as("pocket accounts have no minimum balance")
                .isTrue();
    }

    @Test
    void minimumBalanceVariesByType() {
        Account savings = activeAccount("SAVINGS");
        Account checking = activeAccount("CHECKING");
        Account pocket = activeAccount("POCKET");

        assertThat(savings.canMaintainMinimumBalance(new BigDecimal("999.9999"))).isFalse();
        assertThat(checking.canMaintainMinimumBalance(new BigDecimal("999.9999"))).isFalse();
        assertThat(pocket.canMaintainMinimumBalance(new BigDecimal("999.9999"))).isTrue();
    }

    @Test
    void settersRoundTrip() {
        Account a = activeAccount("SAVINGS");
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        a.setId(id);
        a.setExternalId("ext-new");
        a.setUserId(userId);
        a.setAccountNumber("9876543210");
        a.setAccountType("CHECKING");
        a.setStatus(AccountStatus.FROZEN);
        a.setBalance(new BigDecimal("5.0000"));
        a.setCurrency("USD");
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        a.setVersion(7);

        assertThat(a.getId()).isEqualTo(id);
        assertThat(a.getExternalId()).isEqualTo("ext-new");
        assertThat(a.getUserId()).isEqualTo(userId);
        assertThat(a.getAccountNumber()).isEqualTo("9876543210");
        assertThat(a.getAccountType()).isEqualTo("CHECKING");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(a.getBalance()).isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat(a.getCurrency()).isEqualTo("USD");
        assertThat(a.getCreatedAt()).isEqualTo(now);
        assertThat(a.getUpdatedAt()).isEqualTo(now);
        assertThat(a.getVersion()).isEqualTo(7);
    }

    @Test
    void builderBuildsAccount() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Account a = Account.builder()
                .id(id)
                .externalId("ext-b")
                .userId(userId)
                .accountNumber("1112223334")
                .accountType("SAVINGS")
                .status(AccountStatus.PENDING_VERIFICATION)
                .balance(new BigDecimal("10.0000"))
                .currency("IDR")
                .createdAt(now)
                .updatedAt(now)
                .version(2)
                .build();

        assertThat(a.getId()).isEqualTo(id);
        assertThat(a.getExternalId()).isEqualTo("ext-b");
        assertThat(a.getUserId()).isEqualTo(userId);
        assertThat(a.getAccountNumber()).isEqualTo("1112223334");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(a.getBalance()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(a.getVersion()).isEqualTo(2);
    }

    @Test
    void maximumBalanceGuardRejectsHugeCredit() {
        Account a = activeAccount("SAVINGS");
        assertThatThrownBy(() -> a.credit(new BigDecimal("999999999999.9900")))
                .isInstanceOf(Account.InsufficientFundsException.class);
    }
}
