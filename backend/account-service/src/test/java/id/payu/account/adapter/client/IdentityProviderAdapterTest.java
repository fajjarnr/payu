package id.payu.account.adapter.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACCOUNT-006: IdentityProviderAdapter coverage.
 */
@DisplayName("IdentityProviderAdapter")
class IdentityProviderAdapterTest {

    private final GatewayClient gatewayClient = mock(GatewayClient.class);
    private final IdentityProviderAdapter adapter = new IdentityProviderAdapter(gatewayClient);

    @Test
    void provisionUserReturnsExtractedUserId() {
        when(gatewayClient.registerIdentity(org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("data", Map.of("user_id", "iam-1")));

        String userId = adapter.provisionUser("jdoe", "j@payu.id", "secret", "John Doe");

        assertThat(userId).isEqualTo("iam-1");
    }

    @Test
    void provisionUserReturnsNullWhenMissingUserId() {
        when(gatewayClient.registerIdentity(org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Map.of("data", Map.of("username", "jdoe")));

        assertThat(adapter.provisionUser("jdoe", "j@payu.id", "secret", null)).isNull();
    }

    @Test
    void provisionUserFallbackThrows() throws Exception {
        var m = IdentityProviderAdapter.class.getDeclaredMethod(
                "provisionUserFallback", String.class, String.class, String.class, String.class, Throwable.class);
        m.setAccessible(true);

        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                m.invoke(adapter, "jdoe", "j@payu.id", "secret", "John Doe",
                        new RuntimeException("down")));
        assertThat(thrown.getCause())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Identity provider unavailable");
    }

    @Test
    void deleteUserDelegates() {
        adapter.deleteUser("iam-1");
        verify(gatewayClient).deleteIdentity("iam-1");
    }

    @Test
    void deleteUserFallbackSwallows() throws Exception {
        var m = IdentityProviderAdapter.class.getDeclaredMethod(
                "deleteUserFallback", String.class, Throwable.class);
        m.setAccessible(true);

        m.invoke(adapter, "iam-1", new RuntimeException("down"));
        // no exception thrown (best-effort compensation)
    }

    @Test
    void deleteUserPropagatesGatewayErrorWithoutFallbackOutsideProxy() {
        doThrow(new RuntimeException("down")).when(gatewayClient).deleteIdentity("iam-1");
        assertThatThrownBy(() -> adapter.deleteUser("iam-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("down");
    }
}
