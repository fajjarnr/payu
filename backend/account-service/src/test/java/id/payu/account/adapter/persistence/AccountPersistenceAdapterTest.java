package id.payu.account.adapter.persistence;

import id.payu.account.adapter.persistence.entity.AccountEntity;
import id.payu.account.adapter.persistence.entity.AccountStatus;
import id.payu.account.adapter.persistence.entity.UserEntity;
import id.payu.account.adapter.persistence.repository.AccountRepository;
import id.payu.account.domain.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: AccountPersistenceAdapter coverage.
 */
@DisplayName("AccountPersistenceAdapter")
class AccountPersistenceAdapterTest {

    private final AccountRepository repo = mock(AccountRepository.class);
    private final AccountPersistenceAdapter adapter = new AccountPersistenceAdapter(repo);

    private AccountEntity entity() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        AccountEntity e = new AccountEntity();
        e.setId(UUID.randomUUID());
        e.setUser(user);
        e.setAccountNumber("1234567890");
        e.setType(id.payu.account.adapter.persistence.entity.AccountType.SAVINGS);
        e.setStatus(AccountStatus.ACTIVE);
        e.setBalance(new BigDecimal("1000.0000"));
        e.setCurrency("IDR");
        return e;
    }

    private Account domain() {
        return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("0987654321")
                .accountType("SAVINGS")
                .status(id.payu.account.domain.model.AccountStatus.ACTIVE)
                .balance(new BigDecimal("500.0000"))
                .currency("IDR")
                .build();
    }

    @Test
    void saveMapsAndPersists() {
        Account a = domain();
        when(repo.save(any(AccountEntity.class))).thenAnswer(i -> i.getArgument(0));

        Account saved = adapter.save(a);

        assertThat(saved.getAccountNumber()).isEqualTo("0987654321");
        assertThat(saved.getStatus()).isEqualTo(id.payu.account.domain.model.AccountStatus.ACTIVE);
    }

    @Test
    void findByIdMapsEntity() {
        AccountEntity e = entity();
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        Optional<Account> found = adapter.findById(e.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo("1234567890");
        assertThat(found.get().getUserId()).isEqualTo(e.getUser().getId());
        assertThat(found.get().getStatus()).isEqualTo(id.payu.account.domain.model.AccountStatus.ACTIVE);
    }

    @Test
    void findByExternalIdAndAccountNumber() {
        AccountEntity e = entity();
        when(repo.findByAccountNumber("1234567890")).thenReturn(Optional.of(e));

        assertThat(adapter.findByAccountNumber("1234567890")).isPresent();
    }

    @Test
    void existsAndLookupQueries() {
        UUID lookupUserId = UUID.randomUUID();
        UUID listUserId = UUID.randomUUID();
        when(repo.existsByAccountNumber("1234567890")).thenReturn(true);
        when(repo.findByUserIdAndAllowPhoneLookupTrue(lookupUserId)).thenReturn(Optional.of(entity()));
        when(repo.findByUserId(listUserId)).thenReturn(List.of(entity()));

        assertThat(adapter.existsByAccountNumber("1234567890")).isTrue();
        assertThat(adapter.findByUserIdAndAllowPhoneLookupTrue(lookupUserId)).isPresent();
        assertThat(adapter.findByUserId(listUserId)).hasSize(1);
    }

    @Test
    void mapsNullStatusToPendingVerification() {
        AccountEntity e = entity();
        e.setStatus(null);
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        assertThat(adapter.findById(e.getId()).orElseThrow().getStatus())
                .isEqualTo(id.payu.account.domain.model.AccountStatus.PENDING_VERIFICATION);
    }
}
