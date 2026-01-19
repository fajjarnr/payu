package id.payu.notification.sender;

import id.payu.notification.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * SMS sender (mock implementation).
 * In production, integrate with SMS provider like Twilio, etc.
 */
@ApplicationScoped
public class SmsSender {

    private static final Logger LOG = Logger.getLogger(SmsSender.class);

    public boolean send(Notification notification) {
        try {
            LOG.infof("Sending SMS to: %s", notification.recipient);

            // Mock implementation - in production, call actual SMS API
            // e.g., Twilio, Nexmo, local providers

            LOG.infof("SMS sent successfully to: %s (mock)", notification.recipient);
            return true;
        } catch (Exception e) {
            LOG.errorf("Failed to send SMS: %s", e.getMessage());
            return false;
        }
    }
}
