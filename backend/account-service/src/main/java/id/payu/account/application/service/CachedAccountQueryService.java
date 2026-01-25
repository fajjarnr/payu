package id.payu.account.application.service;

import id.payu.account.domain.model.Account;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.cache.annotation.CacheInvalidate;
import id.payu.cache.annotation.CacheWithTTL;
import id.payu.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cached account query service demonstrating the use of PayU Cache Starter.
 *
 * <p>This service shows two approaches to caching:</p>
 * <ul>
 *   <li>Annotation-based caching using @CacheWithTTL</li>
 *   <li>Programmatic caching using CacheService directly</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CachedAccountQueryService {

    private static final Logger log = LoggerFactory.getLogger(CachedAccountQueryService.class);

    private final CacheService cacheService;
    private final AccountPersistencePort accountPersistencePort;
    private final UserPersistencePort userPersistencePort;

    /**
     * Get account by ID using annotation-based caching with stale-while-revalidate.
     *
     * <p>The cache will:</p>
     * <ul>
     *   <li>Serve fresh data for 5 minutes (soft TTL)</li>
     *   <li>Serve stale data for another 5 minutes while refreshing in background</li>
     *   <li>Force refresh after 10 minutes (hard TTL)</li>
     * </ul>
     */
    @CacheWithTTL(
            cacheName = "accounts",
            key = "'account:' + #accountId",
            ttl = 10,
            timeUnit = TimeUnit.MINUTES,
            softTtlMultiplier = 0.5,
            staleWhileRevalidate = true,
            sync = true
    )
    @Transactional(readOnly = true)
    public Optional<Account> getAccountById(UUID accountId) {
        log.debug("Getting account by ID: {}", accountId);
        return accountPersistencePort.findById(accountId);
    }

    /**
     * Get account by external ID using programmatic caching.
     */
    @Transactional(readOnly = true)
    public Account getAccountByExternalId(String externalId) {
        log.debug("Getting account by external ID: {}", externalId);
        String cacheKey = "account:external:" + externalId;

        return cacheService.get(
                cacheKey,
                Account.class,
                () -> accountPersistencePort.findByExternalId(externalId)
                        .orElseThrow(() -> new AccountNotFoundException(externalId))
        );
    }

    /**
     * Get account balance with aggressive caching (short TTL).
     * Uses stale-while-revalidate for high-traffic balance queries.
     */
    @Transactional(readOnly = true)
    public java.math.BigDecimal getAccountBalance(UUID accountId) {
        log.debug("Getting account balance: {}", accountId);
        String cacheKey = "account:balance:" + accountId;

        return cacheService.getWithStaleWhileRevalidate(
                cacheKey,
                java.math.BigDecimal.class,
                () -> accountPersistencePort.findById(accountId)
                        .map(Account::getBalance)
                        .orElseThrow(() -> new AccountNotFoundException(accountId.toString())),
                Duration.ofSeconds(15),  // Soft TTL - serve stale data
                Duration.ofSeconds(30)   // Hard TTL - must refresh
        );
    }

    /**
     * Get account details with cache refresh on demand.
     * Useful for admin operations where fresh data is needed.
     */
    public Account getAccountWithRefresh(UUID accountId) {
        log.debug("Getting account with refresh: {}", accountId);
        String cacheKey = "account:refresh:" + accountId;

        return cacheService.getAndRefresh(
                cacheKey,
                Account.class,
                () -> accountPersistencePort.findById(accountId)
                        .orElseThrow(() -> new AccountNotFoundException(accountId.toString())),
                Duration.ofSeconds(30),  // Soft TTL
                Duration.ofMinutes(10)  // Hard TTL
        );
    }

    /**
     * Update account and invalidate cache.
     */
    @Transactional
    @CacheInvalidate(
            cacheName = "accounts",
            key = "'account:' + #account.id"
    )
    public Account updateAccount(Account account) {
        log.info("Updating account: {}", account.getId());
        Account updated = accountPersistencePort.save(account);

        // Invalidate related caches
        cacheService.invalidate("account:external:" + account.getExternalId());
        cacheService.invalidate("account:balance:" + account.getId());
        cacheService.invalidate("account:refresh:" + account.getId());

        return updated;
    }

    /**
     * Get user profile with extended cache.
     * User profiles change infrequently, so we use a longer TTL.
     */
    @CacheWithTTL(
            cacheName = "profiles",
            key = "'profile:' + #userId",
            ttl = 30,
            timeUnit = TimeUnit.MINUTES
    )
    @Transactional(readOnly = true)
    public id.payu.account.domain.model.User getUserProfile(UUID userId) {
        log.debug("Getting user profile: {}", userId);
        return userPersistencePort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    /**
     * Check if account exists (lightweight operation).
     */
    public boolean accountExists(UUID accountId) {
        return cacheService.exists("account:id:" + accountId);
    }

    // Custom exceptions
    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String id) {
            super("Account not found: " + id);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String id) {
            super("User not found: " + id);
        }
    }
}
