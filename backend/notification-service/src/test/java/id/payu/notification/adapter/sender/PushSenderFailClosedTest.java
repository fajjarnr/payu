package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROD-044: push has no real provider wired (FCM pending) — without an
 * explicit dev LOG mode it must fail closed, never report SENT for a mock.
 */
class PushSenderFailClosedTest {

    private final PushSender sender = new PushSender();

    private Notification notification() {
        Notification n = new Notification();
        n.setRecipient("device-token-123");
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
    @DisplayName("FCM lab stub succeeds (ADR-0027)")
    void fcmLabSucceeds() {
        sender.pushProvider = "FCM";
        assertThat(sender.send(notification())).isTrue();
    }

    @Test
    @DisplayName("explicit LOG mode still returns success (dev tool)")
    void explicitLogModeSucceeds() {
        sender.pushProvider = "LOG";
        assertThat(sender.send(notification())).isTrue();
    }
}
