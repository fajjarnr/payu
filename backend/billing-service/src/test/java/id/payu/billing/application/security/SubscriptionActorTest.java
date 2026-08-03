package id.payu.billing.application.security;

import id.payu.billing.domain.model.SubscriptionActor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionActorTest {

    @Test
    void partnerMustOwnRequestedPartner() {
        SubscriptionActor actor = new SubscriptionActor(
                "user-1", "account-1", "partner-a", true, false);

        assertTrue(actor.canManagePartner("partner-a"));
        assertFalse(actor.canManagePartner("partner-b"));
    }

    @Test
    void nonPartnerCannotManagePartnerResources() {
        SubscriptionActor actor = new SubscriptionActor(
                "user-1", "account-1", null, false, false);

        assertFalse(actor.canManagePartner("partner-a"));
    }

    @Test
    void accountOwnerCannotAccessAnotherAccount() {
        SubscriptionActor actor = new SubscriptionActor(
                "user-1", "account-1", null, false, false);

        assertTrue(actor.canAccessAccount("account-1"));
        assertFalse(actor.canAccessAccount("account-2"));
    }
}
