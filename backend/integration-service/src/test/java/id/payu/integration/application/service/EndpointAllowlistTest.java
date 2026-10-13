package id.payu.integration.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class EndpointAllowlistTest {

    private final EndpointAllowlist allowlist = new EndpointAllowlist(
            "swift-gateway.payu.fajjjar.my.id, reporting.ojk.go.id");

    @Test
    void allowsConfiguredHosts() {
        assertThatNoException().isThrownBy(
                () -> allowlist.requireAllowed("https://swift-gateway.payu.fajjjar.my.id/api/v1/messages"));
    }

    @Test
    void rejectsUnlistedHost() {
        assertThatThrownBy(() -> allowlist.requireAllowed("https://evil.example.com/hook"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> allowlist.requireAllowed("file:/tmp/ojk-reports/x.csv"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> allowlist.requireAllowed("direct:soap-request"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyAllowlistRejectsEverything() {
        EndpointAllowlist empty = new EndpointAllowlist("");
        assertThatThrownBy(() -> empty.requireAllowed("https://swift-gateway.payu.fajjjar.my.id/x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
