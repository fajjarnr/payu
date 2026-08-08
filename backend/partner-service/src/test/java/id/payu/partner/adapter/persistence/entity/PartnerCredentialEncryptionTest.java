package id.payu.partner.adapter.persistence.entity;

import id.payu.partner.TestSecurityConfig;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.domain.PartnerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PARTNER-PROD-002: partner credentials (clientSecret, apiKey) and webhook
 * signing secrets must be encrypted at rest. A query against the database
 * column must not find the plaintext value — only the ENC(...) ciphertext.
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "payu.security.encryption-enabled=true",
        "payu.security.encryption.password=test-encryption-key-2026-at-least-32-chars-long",
        "payu.security.encryption.salt=test-pbkdf2-salt-for-partner-service-2026"
})
class PartnerCredentialEncryptionTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Autowired
    private EntityManager entityManager;

    private String clientSecret;
    private String apiKey;
    private String webhookSecret;

    @BeforeEach
    void setUp() {
        clientSecret = "sec_" + UUID.randomUUID();
        apiKey = "payu_live_" + UUID.randomUUID().toString().replace("-", "");
        webhookSecret = "whsec_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    @DisplayName("partner clientSecret and apiKey are stored encrypted (ENC(...)), not plaintext")
    @Transactional
    void partnerCredentialsAreEncryptedAtRest() {
        var partner = new PartnerEntity();
        partner.setPartnerCode("ENC-" + System.currentTimeMillis());
        partner.setName("Encryption Test Partner");
        partner.setType("MERCHANT");
        partner.setEmail("enc-" + System.currentTimeMillis() + "@payu.test");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId("cli_" + UUID.randomUUID());
        partner.setClientSecret(clientSecret);
        partner.setApiKey(apiKey);
        partner = partnerRepository.saveAndFlush(partner);

        // In-memory entity reads back the plaintext (decrypted by the converter).
        assertThat(partner.getClientSecret()).isEqualTo(clientSecret);
        assertThat(partner.getApiKey()).isEqualTo(apiKey);

        // Database column must hold ciphertext, never the plaintext.
        Object[] row = (Object[]) entityManager
                .createNativeQuery("SELECT client_secret, api_key FROM partners WHERE id = :id")
                .setParameter("id", partner.getId())
                .getSingleResult();
        String storedSecret = (String) row[0];
        String storedApiKey = (String) row[1];

        assertThat(storedSecret).startsWith("ENC(").doesNotContain(clientSecret);
        assertThat(storedApiKey).startsWith("ENC(").doesNotContain(apiKey);
    }

    @Test
    @DisplayName("webhook subscription secret is stored encrypted (ENC(...)), not plaintext")
    @Transactional
    void webhookSecretIsEncryptedAtRest() {
        var partner = new PartnerEntity();
        partner.setPartnerCode("ENC-WH-" + System.currentTimeMillis());
        partner.setName("Webhook Encryption Partner");
        partner.setType("MERCHANT");
        partner.setEmail("enc-wh-" + System.currentTimeMillis() + "@payu.test");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId("cli_" + UUID.randomUUID());
        partner.setClientSecret("sec-" + UUID.randomUUID());
        partner = partnerRepository.saveAndFlush(partner);

        var subscription = new WebhookSubscriptionEntity(
                partner, "https://partner.example.com/webhook", "payment.completed", webhookSecret);
        subscription = webhookSubscriptionRepository.saveAndFlush(subscription);

        assertThat(subscription.getSecret()).isEqualTo(webhookSecret);

        String storedSecret = (String) entityManager
                .createNativeQuery("SELECT secret FROM webhook_subscriptions WHERE id = :id")
                .setParameter("id", subscription.getId())
                .getSingleResult();

        assertThat(storedSecret).startsWith("ENC(").doesNotContain(webhookSecret);
    }

    @Test
    @DisplayName("clientId (lookup key) stays plaintext at rest")
    @Transactional
    void clientIdRemainsPlaintext() {
        String clientId = "cli_" + UUID.randomUUID();
        var partner = new PartnerEntity();
        partner.setPartnerCode("ENC-CID-" + System.currentTimeMillis());
        partner.setName("ClientId Partner");
        partner.setType("MERCHANT");
        partner.setEmail("enc-cid-" + System.currentTimeMillis() + "@payu.test");
        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setActive(true);
        partner.setClientId(clientId);
        partner.setClientSecret("sec-" + UUID.randomUUID());
        partner = partnerRepository.saveAndFlush(partner);

        String storedClientId = (String) entityManager
                .createNativeQuery("SELECT client_id FROM partners WHERE id = :id")
                .setParameter("id", partner.getId())
                .getSingleResult();

        assertThat(storedClientId).isEqualTo(clientId);
    }
}
