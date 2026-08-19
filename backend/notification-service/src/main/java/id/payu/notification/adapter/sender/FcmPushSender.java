package id.payu.notification.adapter.sender;

import id.payu.notification.domain.Notification;
import id.payu.notification.domain.RecipientMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * FCM v1 REST push sender (ADR-0027). Lab stub that logs and returns true when
 * {@code payu.push.provider=FCM} and projectId is set; otherwise fail-closed is handled by caller.
 * Real impl: POST https://fcm.googleapis.com/v1/projects/{project}/messages:send with OAuth2 bearer.
 * ponytail: stub — add google-auth-library + HttpClient + token cache when FCM credentials exist
 */
@ApplicationScoped
public class FcmPushSender {

    private static final Logger LOG = Logger.getLogger(FcmPushSender.class);

    @ConfigProperty(name = "payu.fcm.project-id")
    java.util.Optional<String> projectId;

    public boolean send(Notification notification) {
        try {
            String pid = projectId != null && projectId.isPresent() ? projectId.get() : null;
            if (pid == null || pid.isBlank() || "not-configured".equals(pid)) {
                LOG.infof("[FCM-SIM] To %s | %s — %s (no project-id, lab mode)", RecipientMasker.mask(notification.getRecipient()), notification.getTitle(), notification.getBody());
                return true;
            }
            LOG.infof("[FCM] Project %s To %s | %s — %s", pid, RecipientMasker.mask(notification.getRecipient()), notification.getTitle(), notification.getBody());
            return true;
        } catch (Exception e) {
            LOG.errorf("FCM send failed: %s", e.getMessage());
            return false;
        }
    }
}
