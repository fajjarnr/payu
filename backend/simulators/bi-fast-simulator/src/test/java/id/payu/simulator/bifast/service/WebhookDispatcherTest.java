package id.payu.simulator.bifast.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookDispatcherTest {

    @Test
    void signsTimestampAndBodyWithTheSharedSecret() {
        assertEquals("c7802a9a1e1932e76af49234ed343f857d38b13a7773a18a9c922eb98985a530",
                WebhookDispatcher.signature(
                        "1700000000", "{\"status\":\"COMPLETED\"}", "secret"));
    }
}
