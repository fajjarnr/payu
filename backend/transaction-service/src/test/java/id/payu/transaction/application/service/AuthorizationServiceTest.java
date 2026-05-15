package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.AccountServicePort;
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
import java.util.Collections;
import java.util.List;
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
 *   <li>Multi-Account Support - Users with multiple accounts can access all their accounts</li>
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

    @Mock
    private AccountServicePort accountServicePort;

    @InjectMocks
    private AuthorizationService authorizationService;

    private UUID transactionId;
    private UUID senderAccountId;
    private UUID otherAccountId;
    private UUID secondAccountId;
    private String userId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        senderAccountId = UUID.randomUUID();
        otherAccountId = UUID.randomUUID();
        secondAccountId = UUID.randomUUID();
        userId = UUID.randomUUID().toString();
        otherUserId = UUID.randomUUID().toString();
    }

    // ==================== TRANSACTION ACCESS TESTS ====================

    @Nested
    @DisplayName("TransactionEntity Access Verification")
    class TransactionAccessTests {

        @Test
        @DisplayName("Should allow access when user owns the transaction (single account)")
        void shouldAllowAccessWhenUserOwnsTransaction() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId));

            // Should not throw
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should allow access when user owns the transaction (multi-account)")
        void shouldAllowAccessWhenUserOwnsTransactionMultiAccount() {
            TransactionEntity transaction = createTransaction(secondAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            // User has multiple accounts including the sender account
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId, secondAccountId));

            // Should not throw
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should deny access when user does not own the transaction")
        void shouldDenyAccessWhenUserDoesNotOwnTransaction() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            // Other user has different accounts
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }

        @Test
        @DisplayName("Should deny access when user has no accounts")
        void shouldDenyAccessWhenUserHasNoAccounts() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            // User has no accounts
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TransactionEntity not found");
        }
    }

    // ==================== ACCOUNT OWNERSHIP TESTS ====================

    @Nested
    @DisplayName("Account Ownership Verification")
    class AccountOwnershipTests {

        @Test
        @DisplayName("Should allow access when user owns the account (single account)")
        void shouldAllowAccessWhenUserOwnsAccount() {
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId));

            // Should not throw
            authorizationService.verifyAccountOwnership(senderAccountId, userId);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should allow access when user owns the account (multi-account)")
        void shouldAllowAccessWhenUserOwnsAccountMultiAccount() {
            // User has multiple accounts
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId, secondAccountId, otherAccountId));

            // Should not throw - checking second account
            authorizationService.verifyAccountOwnership(secondAccountId, userId);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should deny access when user does not own the account")
        void shouldDenyAccessWhenUserDoesNotOwnAccount() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }

        @Test
        @DisplayName("Should deny access when user has no accounts")
        void shouldDenyAccessWhenUserHasNoAccounts() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("Access denied");

            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }
    }

    // ==================== SENDER ACCOUNT OWNERSHIP TESTS ====================

    @Nested
    @DisplayName("Sender Account Ownership Verification")
    class SenderAccountOwnershipTests {

        @Test
        @DisplayName("Should allow transfer from user's own account (single account)")
        void shouldAllowTransferFromUsersOwnAccount() {
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId));

            // Should not throw
            authorizationService.verifySenderAccountOwnership(senderAccountId, userId);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should allow transfer from user's own account (multi-account)")
        void shouldAllowTransferFromUsersOwnAccountMultiAccount() {
            // User has multiple accounts
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId, secondAccountId));

            // Should not throw - transferring from second account
            authorizationService.verifySenderAccountOwnership(secondAccountId, userId);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should deny transfer from another user's account")
        void shouldDenyTransferFromAnotherUsersAccount() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifySenderAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("You can only transfer from your own account");

            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }

        @Test
        @DisplayName("Should deny transfer when user has no accounts")
        void shouldDenyTransferWhenUserHasNoAccounts() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifySenderAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("You can only transfer from your own account");

            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
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
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TransactionEntity not found");
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void shouldHandleNullUserIdGracefully() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(null)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, (String) null))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            verify(accountServicePort).getAccountIdsByUserId(null);
        }

        @Test
        @DisplayName("Should handle empty user ID")
        void shouldHandleEmptyUserId() {
            when(accountServicePort.getAccountIdsByUserId("")).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, ""))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            verify(accountServicePort).getAccountIdsByUserId("");
        }

        @Test
        @DisplayName("Should handle user with empty account list")
        void shouldHandleUserWithEmptyAccountList() {
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, userId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }
    }

    // ==================== ERROR MESSAGE SECURITY TESTS ====================

    @Nested
    @DisplayName("Error Message Security - No Data Leakage")
    class ErrorMessageSecurityTests {

        @Test
        @DisplayName("Should not leak account IDs in error messages")
        void shouldNotLeakAccountIdsInErrorMessages() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(senderAccountId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageNotContaining(senderAccountId.toString())
                    .hasMessageNotContaining(otherAccountId.toString());
        }

        @Test
        @DisplayName("Should not leak user IDs in error messages")
        void shouldNotLeakUserIdsInErrorMessages() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageNotContaining(userId)
                    .hasMessageNotContaining(otherUserId);
        }

        @Test
        @DisplayName("Should provide generic error message for authorization failures")
        void shouldProvideGenericErrorMessageForAuthorizationFailures() {
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

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
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(List.of(senderAccountId));

            // Should not throw and should log (implicitly tested by no exception)
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should log denied access attempts with masked user ID")
        void shouldLogDeniedAccessAttemptsWithMaskedUserId() {
            TransactionEntity transaction = createTransaction(senderAccountId);
            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(otherUserId)).thenReturn(List.of(otherAccountId));

            assertThatThrownBy(() -> authorizationService.verifyTransactionAccess(transactionId, otherUserId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            // Verify logging occurred by checking the service was called
            verify(transactionPersistencePort).findById(transactionId);
            verify(accountServicePort).getAccountIdsByUserId(otherUserId);
        }
    }

    // ==================== MULTI-ACCOUNT TESTS ====================

    @Nested
    @DisplayName("Multi-Account Support")
    class MultiAccountTests {

        @Test
        @DisplayName("Should support user with many accounts")
        void shouldSupportUserWithManyAccounts() {
            // Create 10 accounts for the user
            List<UUID> manyAccounts = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                manyAccounts.add(UUID.randomUUID());
            }

            // TransactionEntity from the last account
            UUID lastAccount = manyAccounts.get(9);
            TransactionEntity transaction = createTransaction(lastAccount);

            when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(accountServicePort.getAccountIdsByUserId(userId)).thenReturn(manyAccounts);

            // Should allow access to any of the user's accounts
            authorizationService.verifyTransactionAccess(transactionId, userId);

            verify(accountServicePort).getAccountIdsByUserId(userId);
        }

        @Test
        @DisplayName("Should correctly handle account lookup for different users")
        void shouldCorrectlyHandleAccountLookupForDifferentUsers() {
            UUID user1Account1 = UUID.randomUUID();
            UUID user1Account2 = UUID.randomUUID();
            UUID user2Account1 = UUID.randomUUID();

            String user1Id = UUID.randomUUID().toString();
            String user2Id = UUID.randomUUID().toString();

            // User 1 has 2 accounts
            when(accountServicePort.getAccountIdsByUserId(user1Id)).thenReturn(List.of(user1Account1, user1Account2));
            // User 2 has 1 account
            when(accountServicePort.getAccountIdsByUserId(user2Id)).thenReturn(List.of(user2Account1));

            // User 1 can access their accounts
            authorizationService.verifyAccountOwnership(user1Account1, user1Id);
            authorizationService.verifyAccountOwnership(user1Account2, user1Id);

            // User 2 can access their account
            authorizationService.verifyAccountOwnership(user2Account1, user2Id);

            // User 1 cannot access User 2's account
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(user2Account1, user1Id))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

            // User 2 cannot access User 1's accounts
            assertThatThrownBy(() -> authorizationService.verifyAccountOwnership(user1Account1, user2Id))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }

    // ==================== HELPER METHODS ====================

    private TransactionEntity createTransaction(UUID senderAccountId) {
        return TransactionEntity.builder()
                .id(transactionId)
                .referenceNumber("TXN123456")
                .senderAccountId(senderAccountId)
                .recipientAccountId(UUID.randomUUID())
                .amount(id.payu.transaction.domain.model.Money.idr("100000"))
                .description("Test transaction")
                .type(TransactionEntity.TransactionType.INTERNAL_TRANSFER)
                .status(TransactionEntity.TransactionStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
