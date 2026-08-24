package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.adapter.persistence.entity.MerchantEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.PaymentLinkEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.repository.MerchantQrPaymentRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.PaymentLinkRepository;
import id.payu.partner.adapter.persistence.repository.WebhookDeliveryRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.domain.KeyEnvironment;
import id.payu.partner.domain.KeyStatus;
import id.payu.partner.domain.MerchantCategory;
import id.payu.partner.interfaces.dto.ApiKeyDTO;
import id.payu.partner.interfaces.dto.PaymentLinkResponse;
import id.payu.partner.interfaces.dto.WebhookSubscriptionDTO;
import id.payu.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * PARTNER-PROD-006: cross-partner negative matrix — partner A must never be
 * able to read or mutate a resource owned by partner B through the service
 * layer, regardless of the {@code partnerId} passed in the request path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cross-partner isolation matrix (PARTNER-PROD-006)")
class PartnerIsolationMatrixTest {

    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private WebhookSubscriptionRepository subscriptionRepository;
    @Mock private WebhookDeliveryRepository deliveryRepository;
    @Mock private PaymentLinkRepository paymentLinkRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantQrPaymentRepository qrPaymentRepository;
    @Mock private WebhookDispatcherService webhookDispatcher;
    @Mock private OutboxService outboxService;

    private ApiKeyService apiKeyService;
    private WebhookService webhookService;
    private PaymentLinkService paymentLinkService;
    private MerchantService merchantService;

    private PartnerEntity partnerA;
    private PartnerEntity partnerB;

    @BeforeEach
    void setUp() {
        partnerA = new PartnerEntity("Partner A", "MERCHANT", "a@payu.dev", "08111", "key-a");
        partnerA.setId(1L);
        partnerA.setActive(true);
        partnerA.setStatus(id.payu.partner.domain.PartnerStatus.ACTIVE);
        partnerB = new PartnerEntity("Partner B", "MERCHANT", "b@payu.dev", "08122", "key-b");
        partnerB.setId(2L);
        partnerB.setActive(true);
        partnerB.setStatus(id.payu.partner.domain.PartnerStatus.ACTIVE);

        apiKeyService = new ApiKeyService(apiKeyRepository, partnerRepository);
        webhookService = new WebhookService(subscriptionRepository, deliveryRepository,
                partnerRepository, new WebhookUrlValidatorService());
        paymentLinkService = new PaymentLinkService(paymentLinkRepository, partnerRepository,
                webhookDispatcher, outboxService);
        merchantService = new MerchantService(merchantRepository, qrPaymentRepository,
                partnerRepository, webhookDispatcher, outboxService, new ObjectMapper());
    }

    @Test
    @DisplayName("api key: partner A cannot get/update/rotate/revoke partner B's key")
    void apiKeyIsolation() {
        ApiKeyEntity keyOfB = new ApiKeyEntity(partnerB, "payu_test_", "hash-b", "abcd", KeyEnvironment.SANDBOX);
        keyOfB.setId(500L);
        keyOfB.setStatus(KeyStatus.ACTIVE);
        when(apiKeyRepository.findById(500L)).thenReturn(Optional.of(keyOfB));

        assertThrows(IllegalArgumentException.class, () -> apiKeyService.getApiKey(1L, 500L));
        assertThrows(IllegalArgumentException.class, () -> apiKeyService.updateApiKey(1L, 500L, new ApiKeyDTO()));
        assertThrows(IllegalArgumentException.class, () -> apiKeyService.rotateApiKey(1L, 500L));
        assertThrows(IllegalArgumentException.class, () -> apiKeyService.revokeApiKey(1L, 500L, "test"));
    }

    @Test
    @DisplayName("webhook: partner A cannot get/update/delete/regenerate partner B's subscription")
    void webhookIsolation() {
        WebhookSubscriptionEntity subOfB = new WebhookSubscriptionEntity(
                partnerB, "https://8.8.8.8/wh", "payment.completed", "secret-b");
        subOfB.setId(600L);
        when(subscriptionRepository.findById(600L)).thenReturn(Optional.of(subOfB));

        assertThrows(IllegalArgumentException.class, () -> webhookService.getSubscription(1L, 600L));
        WebhookSubscriptionDTO update = new WebhookSubscriptionDTO();
        update.setActive(false);
        assertThrows(IllegalArgumentException.class,
                () -> webhookService.updateSubscription(1L, 600L, update));
        assertThrows(IllegalArgumentException.class, () -> webhookService.deleteSubscription(1L, 600L));
        assertThrows(IllegalArgumentException.class, () -> webhookService.regenerateSecret(1L, 600L));
    }

    @Test
    @DisplayName("payment link: partner A cannot get or cancel partner B's link")
    void paymentLinkIsolation() {
        PaymentLinkEntity linkOfB = new PaymentLinkEntity();
        linkOfB.setId(700L);
        linkOfB.setPartner(partnerB);
        linkOfB.setAmount(new BigDecimal("100.00"));
        linkOfB.setCurrency("IDR");
        linkOfB.setStatus(id.payu.partner.domain.PaymentLinkStatus.ACTIVE);
        when(paymentLinkRepository.findById(700L)).thenReturn(Optional.of(linkOfB));

        assertThrows(IllegalArgumentException.class,
                () -> paymentLinkService.getByIdForPartner(1L, 700L));
        assertThrows(IllegalArgumentException.class,
                () -> paymentLinkService.cancelPaymentLink(1L, 700L));
    }

    @Test
    @DisplayName("merchant: partner A cannot get or activate partner B's merchant")
    void merchantIsolation() {
        MerchantEntity merchantOfB = new MerchantEntity(partnerB, "MCH-B-001", "Store B",
                MerchantCategory.RETAIL, "Addr B");
        merchantOfB.setId(800L);
        when(merchantRepository.findById(800L)).thenReturn(Optional.of(merchantOfB));

        assertThrows(IllegalArgumentException.class,
                () -> merchantService.getMerchantForPartner(1L, 800L));
        assertThrows(IllegalArgumentException.class,
                () -> merchantService.activateMerchantForPartner(1L, 800L));
    }
}
