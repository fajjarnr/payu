package id.payu.partner.adapter.persistence;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PARTNER-PROD-002: rewrites legacy plaintext credentials through the entity so
 * {@code EncryptedStringConverter} encrypts them at rest. Rows whose value
 * already starts with {@code ENC(} are skipped. Runs in small batches with a
 * pessimistic write lock so concurrent instances never double-migrate.
 */
@Component
@RequiredArgsConstructor
class CredentialBackfillBatch {

    private static final int BATCH_SIZE = 100;

    private final PartnerRepository partnerRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int migrateNextBatch() {
        int migrated = 0;

        List<PartnerEntity> partners = partnerRepository
                .lockNextPlaintextCredentialBatch(PageRequest.of(0, BATCH_SIZE));
        for (PartnerEntity partner : partners) {
            // Bulk JPQL update always executes and binds through the converter,
            // so the plaintext value is rewritten to ENC(...) at rest.
            migrated += partnerRepository.rewriteEncryptedCredentials(
                    partner.getId(), partner.getClientSecret(), partner.getApiKey());
        }

        List<WebhookSubscriptionEntity> subscriptions = webhookSubscriptionRepository
                .lockNextPlaintextSecretBatch(PageRequest.of(0, BATCH_SIZE));
        for (WebhookSubscriptionEntity subscription : subscriptions) {
            migrated += webhookSubscriptionRepository.rewriteEncryptedSecret(
                    subscription.getId(), subscription.getSecret());
        }

        return migrated;
    }
}
