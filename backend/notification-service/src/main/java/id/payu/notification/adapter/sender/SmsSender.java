package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * SMS sender with configurable provider mode.
 */
@ApplicationScoped
public class SmsSender {

    private static final Logger LOG = Logger.getLogger(SmsSender.class);

    @ConfigProperty(name = "payu.sms.provider", defaultValue = "LOG")
    String smsProvider;

    public boolean send(Notification notification) {
        return switch (smsProvider.toUpperCase()) {
            case "LOG" -> sendViaLog(notification);
            case "TWILIO" -> sendViaTwilio(notification);
            case "VONAGE" -> sendViaVonage(notification);
            case "ZENZIVA" -> sendViaZenziva(notification);
            default -> {
                LOG.warnf("Unknown SMS provider '%s', falling back to LOG mode", smsProvider);
                yield sendViaLog(notification);
            }
        };
    }

    private boolean sendViaLog(Notification notification) {
        LOG.infof("╔══════════════════════════════════════════════════╗");
        LOG.infof("║           📱 SMS (LOG MODE)                     ║");
        LOG.infof("╠══════════════════════════════════════════════════╣");
        LOG.infof("║ To:      %-40s║", notification.getRecipient());
        LOG.infof("║ Subject: %-40s║", notification.getTitle());
        LOG.infof("║ Body:    %-40s║", notification.getBody());
        LOG.infof("╚══════════════════════════════════════════════════╝");
        return true;
    }

    private boolean sendViaTwilio(Notification notification) {
        LOG.warnf("Twilio SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }

    private boolean sendViaVonage(Notification notification) {
        LOG.warnf("Vonage SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }

    private boolean sendViaZenziva(Notification notification) {
        LOG.warnf("Zenziva SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }
}
