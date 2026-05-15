package id.payu.notification.adapter.sender;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * SMS sender with configurable provider mode.
 * <p>
 * Supported modes (set via {@code payu.sms.provider}):
 * <ul>
 *   <li><b>LOG</b> (default) — Logs full SMS content to console. Perfect for lab/dev.</li>
 *   <li><b>TWILIO</b> — Calls Twilio REST API. Free tier: ~$15 trial credit (~0.0079/SMS).</li>
 *   <li><b>VONAGE</b> — Calls Vonage (Nexmo) API. Free tier: €2 credit.</li>
 *   <li><b>ZENZIVA</b> — Indonesian local provider. Cheapest for domestic SMS.</li>
 * </ul>
 * For lab use, LOG mode is recommended — zero cost, all OTP/messages visible in logs.
 */
@ApplicationScoped
public class SmsSender {

    private static final Logger LOG = Logger.getLogger(SmsSender.class);

    @ConfigProperty(name = "payu.sms.provider", defaultValue = "LOG")
    String smsProvider;

    public boolean send(NotificationEntity notification) {
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

    /**
     * LOG mode: prints full SMS content to console.
     * OTP codes and message bodies are fully visible for debugging.
     */
    private boolean sendViaLog(NotificationEntity notification) {
        LOG.infof("╔══════════════════════════════════════════════════╗");
        LOG.infof("║           📱 SMS (LOG MODE)                     ║");
        LOG.infof("╠══════════════════════════════════════════════════╣");
        LOG.infof("║ To:      %-40s║", notification.recipient);
        LOG.infof("║ Subject: %-40s║", notification.title);
        LOG.infof("║ Body:    %-40s║", notification.body);
        LOG.infof("╚══════════════════════════════════════════════════╝");
        return true;
    }

    /**
     * Twilio integration stub.
     * To enable: set payu.sms.provider=TWILIO and configure:
     *   payu.sms.twilio.account-sid, payu.sms.twilio.auth-token, payu.sms.twilio.from-number
     */
    private boolean sendViaTwilio(NotificationEntity notification) {
        // TODO: Implement Twilio REST API call
        // POST https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json
        // Body: To, From, Body
        LOG.warnf("Twilio SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }

    /**
     * Vonage (Nexmo) integration stub.
     * To enable: set payu.sms.provider=VONAGE and configure:
     *   payu.sms.vonage.api-key, payu.sms.vonage.api-secret, payu.sms.vonage.from
     */
    private boolean sendViaVonage(NotificationEntity notification) {
        // TODO: Implement Vonage REST API call
        // POST https://rest.nexmo.com/sms/json
        LOG.warnf("Vonage SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }

    /**
     * Zenziva integration stub — Indonesian local SMS provider (cheapest domestic).
     * To enable: set payu.sms.provider=ZENZIVA and configure:
     *   payu.sms.zenziva.userkey, payu.sms.zenziva.passkey
     */
    private boolean sendViaZenziva(NotificationEntity notification) {
        // TODO: Implement Zenziva API call
        // POST https://console.zenziva.net/wareguler/api/sendWA/
        LOG.warnf("Zenziva SMS provider not yet implemented — falling back to LOG mode");
        return sendViaLog(notification);
    }
}
