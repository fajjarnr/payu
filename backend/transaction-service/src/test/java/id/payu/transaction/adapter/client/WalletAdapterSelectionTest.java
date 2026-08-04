package id.payu.transaction.adapter.client;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletAdapterSelectionTest {

    @Test
    void selectsGrpcAdapterWhenGrpcIntegrationIsEnabled() {
        assertTrue(WalletGrpcAdapter.class.isAnnotationPresent(Primary.class));
        assertFalse(WalletRestAdapter.class.isAnnotationPresent(Primary.class));
    }
}
