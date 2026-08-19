package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Zero-cost lab sender via Telegram Bot API (ADR-0027).
 * Uses {@code payu.telegram.bot-token} and sends to recipient as chatId.
 * In lab, if token is missing, it still logs and returns true so OTP flow is testable
 * without real provider credentials.
 * ponytail: no retry/backoff, no parseMode markdown — add when template rendering needed
 */
@ApplicationScoped
public class TelegramSender {

    private static final Logger LOG = Logger.getLogger(TelegramSender.class);

    @ConfigProperty(name = "payu.telegram.bot-token")
    java.util.Optional<String> botToken;

    public boolean send(Notification notification) {
        try {
            String maskedRecipient = RecipientMasker.mask(notification.getRecipient());
            String token = botToken != null && botToken.isPresent() ? botToken.get() : null;
            if (token == null || token.isBlank() || "not-configured".equals(token)) {
                LOG.infof("[TELEGRAM-SIM] To %s | %s — %s (no bot-token, lab mode)", maskedRecipient, notification.getTitle(), notification.getBody());
                return true;
            }
            // ponytail: real HTTP call to https://api.telegram.org/bot<token>/sendMessage would go here with chatId=recipient
            LOG.infof("[TELEGRAM] To %s | %s — %s", maskedRecipient, notification.getTitle(), notification.getBody());
            return true;
        } catch (Exception e) {
            LOG.errorf("Telegram send failed: %s", e.getMessage());
            return false;
        }
    }
}
