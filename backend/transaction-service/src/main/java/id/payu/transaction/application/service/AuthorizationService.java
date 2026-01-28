package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Service for verifying resource ownership and authorization.
 *
 * Ensures that users can only access resources they own, implementing
 * the principle of least privilege and data-level authorization.
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

    /**
     * Verifies that the user has access to the specified transaction.
     *
     * @param transactionId The transaction ID to check
     * @param userId The user ID requesting access
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the transaction
     */
    public void verifyTransactionAccess(java.util.UUID transactionId, String userId) {
        Transaction transaction = transactionPersistencePort.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        // Check if user owns the sender account associated with this transaction
        if (!transaction.getSenderAccountId().toString().equals(extractAccountIdFromUserId(userId))) {
            log.warn("User {} attempted to access transaction {} belonging to account {}",
                    maskUserId(userId), transactionId, transaction.getSenderAccountId());
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You do not have permission to access this transaction");
        }
    }

    /**
     * Verifies that the user owns the specified account.
     *
     * @param accountId The account ID to check
     * @param userId The user ID requesting access
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the account
     */
    public void verifyAccountOwnership(java.util.UUID accountId, String userId) {
        // Extract account ID from user context and verify ownership
        // This is a simplified version - in production, you'd query an account service
        String userAccountId = extractAccountIdFromUserId(userId);

        if (!accountId.toString().equals(userAccountId)) {
            log.warn("User {} attempted to access account {}",
                    maskUserId(userId), accountId);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You do not have permission to access this account");
        }
    }

    /**
     * Verifies that the sender account in the request belongs to the user.
     *
     * @param senderAccountId The sender account ID from the request
     * @param userId The authenticated user ID
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the account
     */
    public void verifySenderAccountOwnership(java.util.UUID senderAccountId, String userId) {
        String userAccountId = extractAccountIdFromUserId(userId);

        if (!senderAccountId.toString().equals(userAccountId)) {
            log.warn("User {} attempted to transfer from account {}",
                    maskUserId(userId), senderAccountId);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You can only transfer from your own account");
        }
    }

    /**
     * Extracts account ID from user ID or context.
     * In production, this would query a user service or parse JWT claims.
     *
     * @param userId The user ID
     * @return The account ID associated with the user
     */
    private String extractAccountIdFromUserId(String userId) {
        // TODO: Implement proper account ID extraction from JWT or user service
        // For now, return the userId as-is assuming it contains account info
        // In production: userClient.getAccounts(userId).getPrimaryAccountId()
        return userId;
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
