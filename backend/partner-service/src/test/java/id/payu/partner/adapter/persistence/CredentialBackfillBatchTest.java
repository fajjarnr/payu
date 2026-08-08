package id.payu.partner.adapter.persistence;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.domain.PartnerStatus;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PARTNER-PROD-002: the credential backfill re-saves legacy plaintext rows
 * through the entity (so the EncryptedStringConverter encrypts them) and
 * claims rows with a pessimistic write lock for multi-pod safety.
 */
class CredentialBackfillBatchTest {

    @Test
    void claimsPlaintextRowsAndRewritesThroughConverter() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        WebhookSubscriptionRepository webhookRepository = mock(WebhookSubscriptionRepository.class);

        PartnerEntity plainPartner = new PartnerEntity();
        plainPartner.setId(1L);
        plainPartner.setPartnerCode("LEGACY-1");
        plainPartner.setName("Legacy Partner");
        plainPartner.setType("MERCHANT");
        plainPartner.setEmail("legacy@payu.test");
        plainPartner.setStatus(PartnerStatus.ACTIVE);
        plainPartner.setClientSecret("legacy-plaintext-secret");
        plainPartner.setApiKey("legacy-plaintext-key");

        WebhookSubscriptionEntity plainSubscription = new WebhookSubscriptionEntity(
                plainPartner, "https://legacy.example.com/hook", "payment.completed",
                "legacy-plaintext-webhook-secret");

        when(partnerRepository.lockNextPlaintextCredentialBatch(any(Pageable.class)))
                .thenReturn(List.of(plainPartner));
        when(webhookRepository.lockNextPlaintextSecretBatch(any(Pageable.class)))
                .thenReturn(List.of(plainSubscription));
        when(partnerRepository.rewriteEncryptedCredentials(1L, "legacy-plaintext-secret",
                "legacy-plaintext-key")).thenReturn(1);
        when(webhookRepository.rewriteEncryptedSecret(any(), eq("legacy-plaintext-webhook-secret")))
                .thenReturn(1);

        int migrated = new CredentialBackfillBatch(partnerRepository, webhookRepository)
                .migrateNextBatch();

        assertEquals(2, migrated);
        verify(partnerRepository).rewriteEncryptedCredentials(1L, "legacy-plaintext-secret",
                "legacy-plaintext-key");
        verify(webhookRepository).rewriteEncryptedSecret(any(), eq("legacy-plaintext-webhook-secret"));
    }

    @Test
    void returnsZeroWhenNothingToMigrate() {
        PartnerRepository partnerRepository = mock(PartnerRepository.class);
        WebhookSubscriptionRepository webhookRepository = mock(WebhookSubscriptionRepository.class);

        when(partnerRepository.lockNextPlaintextCredentialBatch(any(Pageable.class)))
                .thenReturn(List.of());
        when(webhookRepository.lockNextPlaintextSecretBatch(any(Pageable.class)))
                .thenReturn(List.of());

        int migrated = new CredentialBackfillBatch(partnerRepository, webhookRepository)
                .migrateNextBatch();

        assertEquals(0, migrated);
        verify(partnerRepository, never()).rewriteEncryptedCredentials(any(), any(), any());
        verify(webhookRepository, never()).rewriteEncryptedSecret(any(), any());
    }

    @Test
    void queriesUsePessimisticWriteLockForConcurrentSchedulers() throws Exception {
        Lock partnerLock = PartnerRepository.class
                .getMethod("lockNextPlaintextCredentialBatch", Pageable.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, partnerLock.value());

        Lock webhookLock = WebhookSubscriptionRepository.class
                .getMethod("lockNextPlaintextSecretBatch", Pageable.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, webhookLock.value());
    }

    @Test
    void scheduledRunnerRetriesOnNextInvocationAfterFailure() {
        CredentialBackfillBatch batch = mock(CredentialBackfillBatch.class);
        when(batch.migrateNextBatch())
                .thenThrow(new IllegalStateException("lock conflict"))
                .thenReturn(1);
        CredentialBackfillRunner runner = new CredentialBackfillRunner(batch);

        runner.runNextBatch();
        runner.runNextBatch();

        verify(batch, times(2)).migrateNextBatch();
    }
}

