package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Push notification sender.
 * <p>
 * PROD-044: fail-closed by default. FCM is not wired yet; without an
 * explicit dev {@code LOG} mode, {@link #send} returns {@code false} so a
 * notification is never reported SENT when nothing was delivered.
 */
@ApplicationScoped
public class PushSender {

    private static final Logger LOG = Logger.getLogger(PushSender.class);

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "payu.push.provider", defaultValue = "NONE")
    String pushProvider;

    public boolean send(Notification notification) {
        return switch (pushProvider == null ? "NONE" : pushProvider.toUpperCase()) {
            case "LOG" -> sendViaLog(notification);
            case "NONE" -> failClosed(notification);
            case "FCM" -> {
                LOG.warnf("FCM push provider is not implemented yet — failing closed");
                yield failClosed(notification);
            }
            default -> {
                LOG.warnf("Unknown push provider '%s' — failing closed", pushProvider);
                yield failClosed(notification);
            }
        };
    }

    private boolean failClosed(Notification notification) {
        LOG.errorf("Push not delivered: no working provider configured (payu.push.provider='%s') — " +
                "fail-closed, notification will be retried then marked FAILED", pushProvider);
        return false;
    }

    private boolean sendViaLog(Notification notification) {
        try {
            LOG.infof("Sending push notification to device: %s", RecipientMasker.mask(notification.getRecipient()));
            LOG.infof("Push notification sent (LOG MODE)");
            return true;
        } catch (Exception e) {
            LOG.errorf("Failed to send push notification: %s", e.getMessage());
            return false;
        }
    }
}
