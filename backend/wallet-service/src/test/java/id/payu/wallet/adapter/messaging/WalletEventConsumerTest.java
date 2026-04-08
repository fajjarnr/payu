package id.payu.wallet.adapter.messaging;

import id.payu.wallet.domain.port.in.WalletUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletEventConsumerTest {

    @Mock
    private WalletUseCase walletUseCase;

    @InjectMocks
    private WalletEventConsumer walletEventConsumer;

    @Test
    @DisplayName("Should use externalId when provisioning wallet from user.created event")
    void shouldUseExternalIdWhenProvisioningWallet() {
        walletEventConsumer.consumeUserCreatedEvent(Map.of(
                "userId", "domain-user-id",
                "externalId", "iam-user-id"
        ));

        verify(walletUseCase).createWallet("iam-user-id");
    }

    @Test
    @DisplayName("Should fall back to userId when externalId is missing")
    void shouldFallbackToUserIdWhenExternalIdIsMissing() {
        walletEventConsumer.consumeUserCreatedEvent(Map.of("userId", "domain-user-id"));

        verify(walletUseCase).createWallet("domain-user-id");
    }
}