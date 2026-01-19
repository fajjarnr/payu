package id.payu.notification.sender;

import id.payu.notification.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Push notification sender (mock implementation).
 * In production, integrate with Firebase Cloud Messaging (FCM).
 */
@ApplicationScoped
public class PushSender {

    private static final Logger LOG = Logger.getLogger(PushSender.class);

    public boolean send(Notification notification) {
        try {
            LOG.infof("Sending push notification to device: %s", notification.recipient);

            // Mock implementation - in production, use Firebase Admin SDK
            // FirebaseMessaging.getInstance().send(message);

            LOG.infof("Push notification sent successfully (mock)");
            return true;
        } catch (Exception e) {
            LOG.errorf("Failed to send push notification: %s", e.getMessage());
            return false;
        }
    }
}
