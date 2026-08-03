package id.payu.billing.domain.model;

import java.util.Objects;

/**
 * Authenticated subject passed from the web adapter to subscription use cases.
 */
public record SubscriptionActor(
        String subject,
        String accountId,
        String partnerId,
        boolean partner,
        boolean privileged) {

    public SubscriptionActor {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Authenticated subject is required");
        }
    }

    public boolean canManagePartner(String requestedPartnerId) {
        return privileged || (partner && Objects.equals(partnerId, requestedPartnerId));
    }

    public boolean canAccessAccount(String requestedAccountId) {
        return privileged || Objects.equals(accountId, requestedAccountId);
    }
}
