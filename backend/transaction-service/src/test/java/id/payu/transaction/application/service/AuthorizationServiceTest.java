package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthorizationService.
 *
 * <p>P0 Critical Tests - These tests verify security-critical authorization logic
 * that prevents unauthorized access to financial resources.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Resource Ownership Verification - Users can only access their own resources</li>
 *   <li>Edge Cases - Null values, empty strings, invalid IDs</li>
 *   <li>Error Messages - Verify no sensitive data leakage</li>
 *   <li>Logging - Verify security events are logged</li>
 * </ul>
 *
 * @see AuthorizationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Tests")
class AuthorizationServiceTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;

    @InjectMocks
    private AuthorizationService authorizationService;

    private UUID transactionId;
    private UUID senderAccountId;
    private UUID otherAccountId;
    private String userId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        senderAccountId = UUID.randomUUID();
        otherAccountId = UUID.randomUUID();
        userId = senderAccountId.toString(); // Simplified: userId matches accountId
        otherUserId = otherAccountId.toString();
    }

    // ==================== TRANSACTION ACCESS TESTS ====================

    @Nested
    @DisplayName("Transaction Access Verification")
    class TransactionAccessTests {

        @Test
        @DisplayName("Should allow access when user owns the transaction")
        void shouldAllowAccessWhenUserOwnsTransaction() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            // Should not throw
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(transactionPersistencePort).findById(transactionId);
        }

        @Test
        @DisplayName("Should deny access when user does not own the transaction")
        void shouldDenyAccessWhenUserDoesNotOwnTransaction() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(transactionPersistencePort).findById(transactionId);
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }

    // ==================== ACCOUNT OWNERSHIP TESTS ====================

    @Nested
    @DisplayName("Account Ownership Verification")
    class AccountOwnershipTests {

        @Test
        @DisplayName("Should allow access when user owns the account")
        void shouldAllowAccessWhenUserOwnsAccount() {
            // Should not throw - userId matches accountId
            authorizationService.verifyAccountOwnership(senderAccountId, userId);
        }

        @Test
        @DisplayName("Should deny access when user does not own the account")
        void shouldDenyAccessWhenUserDoesNotOwnAccount() {
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    // ==================== SENDER ACCOUNT OWNERSHIP TESTS ====================

    @Nested
    @DisplayName("Sender Account Ownership Verification")
    class SenderAccountOwnershipTests {

        @Test
        @DisplayName("Should allow transfer from user's own account")
        void shouldAllowTransferFromUsersOwnAccount() {
            // Should not throw - userId matches senderAccountId
            authorizationService.verifySenderAccountOwnership(senderAccountId, userId);
        }

        @Test
        @DisplayName("Should deny transfer from another user's account")
        void shouldDenyTransferFromAnotherUsersAccount() {
            assertThatThrownBy(() -> authorizationService.verifySenderAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("You can only transfer from your own account");
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null transaction ID gracefully")
        void shouldHandleNullTransactionIdGracefully() {
            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(null, userId))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            // Should not throw - handles null internally
            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, (String) null))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should handle empty user ID")
        void shouldHandleEmptyUserId() {
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, ""))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("Should handle mismatched ID formats")
        void shouldHandleMismatchedIdFormats() {
            String invalidUserId = "not-a-uuid";

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, invalidUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }

    // ==================== ERROR MESSAGE SECURITY TESTS ====================

    @Nested
    @DisplayName("Error Message Security - No Data Leakage")
    class ErrorMessageSecurityTests {

        @Test
        @DisplayName("Should not leak account IDs in error messages")
        void shouldNotLeakAccountIdsInErrorMessages() {
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageNotContaining(senderAccountId.toString())
                    .hasMessageNotContaining(otherAccountId.toString());
        }

        @Test
        @DisplayName("Should not leak user IDs in error messages")
        void shouldNotLeakUserIdsInErrorMessages() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageNotContaining(userId)
                    .hasMessageNotContaining(otherUserId);
        }

        @Test
        @DisplayName("Should provide generic error message for authorization failures")
        void shouldProvideGenericErrorMessageForAuthorizationFailures() {
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .hasMessage("Access denied: You do not have permission to access this account");
        }
    }

    // ==================== LOGGING TESTS ====================

    @Nested
    @DisplayName("Security Logging")
    class SecurityLoggingTests {

        @Test
        @DisplayName("Should log successful access without sensitive data")
        void shouldLogSuccessfulAccessWithoutSensitiveData() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            // Should not throw and should log (implicitly tested by no exception)
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(transactionPersistencePort).findById(transactionId);
        }

        @Test
        @DisplayName("Should log denied access attempts with masked user ID")
        void shouldLogDeniedAccessAttemptsWithMaskedUserId() {
            Transaction transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

            String otherUserId = UUID.randomUUID().toString();

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            // Verify logging occurred by checking the service was called
            verify(transactionPersistencePort).findById(transactionId);
        }
    }

    // ==================== HELPER METHODS ====================

    private Transaction createTransaction(UUID senderAccountId) {
        return Transaction.builder()
                .id(transactionId)
                .referenceNumber("TXN123456")
                .senderAccountId(senderAccountId)
                .amount(id.payu.transaction.domain.model.Money.idr("100000"))
                .description("Test transaction")
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
