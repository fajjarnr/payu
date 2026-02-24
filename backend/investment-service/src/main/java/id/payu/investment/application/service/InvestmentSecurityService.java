package id.payu.investment.application.service;

import id.payu.investment.domain.port.out.InvestmentPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for investment operations.
 * Validates ownership and access permissions for investment accounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentSecurityService {

    private final InvestmentPersistencePort investmentPersistencePort;

    /**
     * Checks if the given user is the owner of the investment account.
     *
     * @param accountId the investment account ID
     * @param userId the user ID to check
     * @return true if the user owns the account, false otherwise
     */
    public boolean isAccountOwner(String accountId, String userId) {
        if (accountId == null || userId == null) {
            return false;
        }
        try {
            UUID accountUuid = UUID.fromString(accountId);
            return investmentPersistencePort.findAccountById(accountUuid)
                    .map(account -> account.getUserId().equals(userId))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid account ID format: {}", accountId);
            return false;
        }
    }
}
