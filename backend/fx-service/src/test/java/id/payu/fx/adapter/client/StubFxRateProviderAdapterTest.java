package id.payu.fx.adapter.client;

import id.payu.fx.application.service.FxRateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubFxRateProviderAdapterTest {

    private final StubFxRateProviderAdapter adapter = new StubFxRateProviderAdapter();

    @Test
    void shouldRejectUnknownCurrencyPairInsteadOfReturningFallbackRate() {
        assertThatThrownBy(() -> adapter.fetchCurrentRate("USD", "XYZ"))
                .isInstanceOf(FxRateNotFoundException.class)
                .hasMessageContaining("No stub rate available for USD-XYZ");
    }

    @Test
    void shouldBeLimitedToLocalProfile() {
        Profile profile = StubFxRateProviderAdapter.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("local");
    }
}
