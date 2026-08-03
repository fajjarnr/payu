package id.payu.fx.adapter.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnavailableFxRateProviderAdapterTest {

    private final UnavailableFxRateProviderAdapter adapter = new UnavailableFxRateProviderAdapter();

    @Test
    void shouldFailClosedWhenNoProductionProviderIsConfigured() {
        assertThat(adapter.isAvailable()).isFalse();
        assertThatThrownBy(() -> adapter.fetchCurrentRate("USD", "IDR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No production FX rate provider configured");
    }
}
