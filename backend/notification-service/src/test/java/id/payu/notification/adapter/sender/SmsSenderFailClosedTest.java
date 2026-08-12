package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROD-044: without an explicitly configured real provider the sender must
 * FAIL CLOSED (return false) — a notification must never be reported SENT
 * when nothing was actually delivered. LOG mode is a dev tool, enabled
 * explicitly only.
 */
class SmsSenderFailClosedTest {

    private final SmsSender sender = new SmsSender();

    private Notification notification() {
        Notification n = new Notification();
        n.setRecipient("+6281234567890");
        n.setTitle("Test");
        n.setBody("Test body");
        n.setUserId("user-123");
        return n;
    }

    @Test
    @DisplayName("default (NONE) provider fails closed")
    void defaultProviderFailsClosed() {
        assertThat(sender.send(notification())).isFalse();
    }

    @Test
    @DisplayName("unknown provider fails closed instead of falling back to LOG")
    void unknownProviderFailsClosed() {
        sender.smsProvider = "MAILGUN";
        assertThat(sender.send(notification())).isFalse();
    }

    @Test
    @DisplayName("unimplemented real providers (TWILIO/VONAGE/ZENZIVA) fail closed")
    void unimplementedProvidersFailClosed() {
        for (String provider : new String[] {"TWILIO", "VONAGE", "ZENZIVA"}) {
            sender.smsProvider = provider;
            assertThat(sender.send(notification()))
                    .as("provider %s must not fake success", provider)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("explicit LOG mode still returns success (dev tool)")
    void explicitLogModeSucceeds() {
        sender.smsProvider = "LOG";
        assertThat(sender.send(notification())).isTrue();
    }
}
