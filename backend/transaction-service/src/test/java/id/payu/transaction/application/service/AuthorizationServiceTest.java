package id.payu.transaction.application.service;

import id.payu.api.common.exception.BusinessException;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.port.out.AccountServicePort;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Unit Tests")
class AuthorizationServiceTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;

    @Mock
    private AccountServicePort accountServicePort;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("Missing transaction maps to TXN_404 business error, not IllegalArgumentException")
    void verifyTransactionAccessShouldThrowTxn404ForMissingTransaction() {
        when(transactionPersistencePort.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authorizationService.verifyTransactionAccess(UUID.randomUUID(), "user-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TransactionEntity not found");
        try {
            authorizationService.verifyTransactionAccess(UUID.randomUUID(), "user-1");
        } catch (BusinessException e) {
            assertThat(e.getCode()).isEqualTo("TXN_404");
        }
    }
}
