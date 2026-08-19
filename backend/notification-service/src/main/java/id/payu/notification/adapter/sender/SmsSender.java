package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * SMS sender with configurable provider mode.
 * <p>
 * PROD-044: fail-closed by default. Without an explicitly configured real
 * provider ({@code payu.sms.provider=TWILIO|VONAGE|ZENZIVA}) or an explicit
 * dev {@code LOG} mode, {@link #send} returns {@code false} — a notification
 * must never be reported SENT when nothing was delivered.
 */
@ApplicationScoped
public class SmsSender {

    private static final Logger LOG = Logger.getLogger(SmsSender.class);

    @ConfigProperty(name = "payu.sms.provider", defaultValue = "NONE")
    String smsProvider;

    @Inject
    TelegramSender telegramSender;

    @Inject
    SmsSimulatorSender simulatorSender;

    public boolean send(Notification notification) {
        return switch (smsProvider == null ? "NONE" : smsProvider.toUpperCase()) {
            case "LOG" -> sendViaLog(notification);
            case "TELEGRAM" -> telegramSender != null ? telegramSender.send(notification) : labLog("TELEGRAM", notification);
            case "SIMULATOR" -> simulatorSender != null ? simulatorSender.send(notification) : labLog("SIMULATOR", notification);
            case "NONE" -> failClosed(notification);
            case "TWILIO", "VONAGE", "ZENZIVA" -> {
                LOG.warnf("SMS provider '%s' is not implemented yet — failing closed", smsProvider);
                yield failClosed(notification);
            }
            default -> {
                LOG.warnf("Unknown SMS provider '%s' — failing closed", smsProvider);
                yield failClosed(notification);
            }
        };
    }

    private boolean labLog(String provider, Notification n) {
        LOG.infof("[%s-SIM] To %s | %s — %s (no CDI, lab mode)", provider, RecipientMasker.mask(n.getRecipient()), n.getTitle(), n.getBody());
        return true;
    }

    private boolean failClosed(Notification notification) {
        LOG.errorf("SMS not delivered: no working provider configured (payu.sms.provider='%s') — " +
                "fail-closed, notification will be retried then marked FAILED", smsProvider);
        return false;
    }

    private boolean sendViaLog(Notification notification) {
        LOG.infof("╔══════════════════════════════════════════════════╗");
        LOG.infof("║           📱 SMS (LOG MODE)                     ║");
        LOG.infof("╠══════════════════════════════════════════════════╣");
        LOG.infof("║ To:      %-40s║", RecipientMasker.mask(notification.getRecipient()));
        LOG.infof("╚══════════════════════════════════════════════════╝");
        return true;
    }
}
