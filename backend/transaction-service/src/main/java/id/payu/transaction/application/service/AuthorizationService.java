package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.AccountServicePort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Service for verifying resource ownership and authorization.
 *
 * Ensures that users can only access resources they own, implementing
 * the principle of least privilege and data-level authorization.
 *
 * <p>Multi-Account Support:</p>
 * <ul>
 *   <li>Users may have multiple accounts (savings, checking, pockets)</li>
 *   <li>Authorization checks against all user accounts, not just primary</li>
 *   <li>Account IDs are fetched from account-service via {@link AccountServicePort}</li>
 * </ul>
 *
 * PCI-DSS Compliance:
 * - Requirement 7: Restrict access to cardholder data by business need-to-know
 * - OWASP: Verify authorization for every data access
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationService {

    private final TransactionPersistencePort transactionPersistencePort;
    private final AccountServicePort accountServicePort;

    /**
     * Verifies that the user has access to the specified transaction.
     *
     * <p>Supports multi-account scenarios by checking if the transaction's
     * sender account belongs to any of the user's accounts.</p>
     *
     * @param transactionId The transaction ID to check
     * @param userId The user ID requesting access
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the transaction
     */
    public void verifyTransactionAccess(java.util.UUID transactionId, String userId) {
        Transaction transaction = transactionPersistencePort.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // Get all account IDs for the user (multi-account support)
        List<UUID> userAccountIds = accountServicePort.getAccountIdsByUserId(userId);

        // Check if user owns the sender account associated with this transaction
        if (!userAccountIds.contains(transaction.getSenderAccountId())) {
            log.warn("User {} attempted to access transaction {} belonging to account {}",
                    maskUserId(userId), transactionId, transaction.getSenderAccountId());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You do not have permission to access this transaction");
        }
    }

    /**
     * Verifies that the user owns the specified account.
     *
     * <p>Supports multi-account scenarios by checking against all accounts
     * associated with the user.</p>
     *
     * @param accountId The account ID to check
     * @param userId The user ID requesting access
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the account
     */
    public void verifyAccountOwnership(java.util.UUID accountId, String userId) {
        // Get all account IDs for the user (multi-account support)
        List<UUID> userAccountIds = accountServicePort.getAccountIdsByUserId(userId);

        if (!userAccountIds.contains(accountId)) {
            log.warn("User {} attempted to access account {}",
                    maskUserId(userId), accountId);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You do not have permission to access this account");
        }
    }

    /**
     * Verifies that the sender account in the request belongs to the user.
     *
     * <p>Supports multi-account scenarios by checking against all accounts
     * associated with the user.</p>
     *
     * @param senderAccountId The sender account ID from the request
     * @param userId The authenticated user ID
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the account
     */
    public void verifySenderAccountOwnership(java.util.UUID senderAccountId, String userId) {
        // Get all account IDs for the user (multi-account support)
        List<UUID> userAccountIds = accountServicePort.getAccountIdsByUserId(userId);

        if (!userAccountIds.contains(senderAccountId)) {
            log.warn("User {} attempted to transfer from account {}",
                    maskUserId(userId), senderAccountId);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You can only transfer from your own account");
        }
    }

    /**
     * Masks user ID for safe logging.
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "***";
        }
        return userId.substring(0, 4) + "***";
    }
}
