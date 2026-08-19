package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Zero-cost lab SMS simulator (ADR-0027). No external call — logs and returns true.
 * ponytail: in-memory only, no DLQ; add file/DB sink if lab audit needs persistence
 */
@ApplicationScoped
public class SmsSimulatorSender {

    private static final Logger LOG = Logger.getLogger(SmsSimulatorSender.class);

    public boolean send(Notification notification) {
        LOG.infof("[SMS-SIM] To %s | %s — %s", RecipientMasker.mask(notification.getRecipient()), notification.getTitle(), notification.getBody());
        return true;
    }
}
