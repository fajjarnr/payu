package id.payu.account.application.service;

import id.payu.account.domain.model.Account;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.cache.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: CachedAccountQueryService coverage.
 */
@DisplayName("CachedAccountQueryService")
class CachedAccountQueryServiceTest {

    private final CacheService cacheService = mock(CacheService.class);
    private final AccountPersistencePort accountPort = mock(AccountPersistencePort.class);
    private final UserPersistencePort userPort = mock(UserPersistencePort.class);
    private final CachedAccountQueryService service =
            new CachedAccountQueryService(cacheService, accountPort, userPort);

    private Account account() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setExternalId("ext-1");
        a.setBalance(new BigDecimal("50000.0000"));
        a.setCurrency("IDR");
        return a;
    }

    @Test
    void getAccountByIdDelegatesToPort() {
        Account a = account();
        when(accountPort.findById(a.getId())).thenReturn(Optional.of(a));

        assertThat(service.getAccountById(a.getId())).contains(a);
    }

    @Test
    void getAccountByExternalIdUsesCache() {
        Account a = account();
        when(cacheService.get(eq("account:external:ext-1"), eq(Account.class), any()))
                .thenReturn(a);

        assertThat(service.getAccountByExternalId("ext-1")).isEqualTo(a);
    }

    @Test
    void getAccountByExternalIdThrowsWhenMissing() {
        when(cacheService.get(eq("account:external:missing"), eq(Account.class), any()))
                .thenThrow(new CachedAccountQueryService.AccountNotFoundException("missing"));

        assertThatThrownBy(() -> service.getAccountByExternalId("missing"))
                .isInstanceOf(CachedAccountQueryService.AccountNotFoundException.class);
    }

    @Test
    void getAccountBalanceFromPort() {
        Account a = account();
        when(cacheService.getWithStaleWhileRevalidate(
                eq("account:balance:" + a.getId()), eq(BigDecimal.class), any(),
                eq(Duration.ofSeconds(15)), eq(Duration.ofSeconds(30))))
                .thenReturn(a.getBalance());

        assertThat(service.getAccountBalance(a.getId())).isEqualByComparingTo(new BigDecimal("50000.0000"));
    }

    @Test
    void getAccountWithRefresh() {
        Account a = account();
        when(cacheService.getAndRefresh(eq("account:refresh:" + a.getId()), eq(Account.class), any(),
                eq(Duration.ofSeconds(30)), eq(Duration.ofMinutes(10))))
                .thenReturn(a);

        assertThat(service.getAccountWithRefresh(a.getId())).isEqualTo(a);
    }

    @Test
    void updateAccountSavesAndInvalidates() {
        Account a = account();
        when(accountPort.save(a)).thenReturn(a);

        Account updated = service.updateAccount(a);

        assertThat(updated).isEqualTo(a);
        verify(cacheService).invalidate("account:external:ext-1");
        verify(cacheService).invalidate("account:balance:" + a.getId());
        verify(cacheService).invalidate("account:refresh:" + a.getId());
    }

    @Test
    void getUserProfileFoundAndMissing() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        when(userPort.findById(userId)).thenReturn(Optional.of(user));
        assertThat(service.getUserProfile(userId)).isEqualTo(user);

        UUID missing = UUID.randomUUID();
        when(userPort.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUserProfile(missing))
                .isInstanceOf(CachedAccountQueryService.UserNotFoundException.class);
    }

    @Test
    void accountExistsProbesCache() {
        UUID id = UUID.randomUUID();
        when(cacheService.exists("account:id:" + id)).thenReturn(true);
        assertThat(service.accountExists(id)).isTrue();
    }

    @Test
    void getAccountByExternalIdThrowsWhenPortMisses() {
        UUID id = UUID.randomUUID();
        when(accountPort.findByExternalId("missing")).thenReturn(Optional.empty());
        when(cacheService.get(eq("account:external:missing"), eq(Account.class), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(2)).get());

        assertThatThrownBy(() -> service.getAccountByExternalId("missing"))
                .isInstanceOf(CachedAccountQueryService.AccountNotFoundException.class);
    }

    @Test
    void getAccountBalanceThrowsWhenPortMisses() {
        UUID id = UUID.randomUUID();
        when(accountPort.findById(id)).thenReturn(Optional.empty());
        when(cacheService.getWithStaleWhileRevalidate(
                eq("account:balance:" + id), eq(BigDecimal.class), any(),
                eq(Duration.ofSeconds(15)), eq(Duration.ofSeconds(30))))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(2)).get());

        assertThatThrownBy(() -> service.getAccountBalance(id))
                .isInstanceOf(CachedAccountQueryService.AccountNotFoundException.class);
    }
}
