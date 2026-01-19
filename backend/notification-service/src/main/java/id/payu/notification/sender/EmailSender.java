package id.payu.notification.sender;

import id.payu.notification.domain.Notification;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Email sender using Quarkus Mailer.
 */
@ApplicationScoped
public class EmailSender {

    private static final Logger LOG = Logger.getLogger(EmailSender.class);

    @Inject
    Mailer mailer;

    public boolean send(Notification notification) {
        try {
            LOG.infof("Sending email to: %s", notification.recipient);

            mailer.send(
                    Mail.withText(
                            notification.recipient,
                            notification.title,
                            notification.body));

            LOG.infof("Email sent successfully to: %s", notification.recipient);
            return true;
        } catch (Exception e) {
            LOG.errorf("Failed to send email: %s", e.getMessage());
            return false;
        }
    }
}
