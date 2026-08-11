package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.repository.WebhookDeliveryRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookDeliveryEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.dto.WebhookDeliveryDTO;
import id.payu.partner.dto.WebhookSubscriptionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private WebhookUrlValidatorService webhookUrlValidator;

    @InjectMocks
    private WebhookService webhookService;

    private PartnerEntity activePartner;
    private PartnerEntity inactivePartner;
    private WebhookSubscriptionEntity testSubscription;

    @BeforeEach
    void setUp() {
        activePartner = new PartnerEntity();
        activePartner.setId(1L);
        activePartner.setName("TokoBapak");
        activePartner.setType("MERCHANT");
        activePartner.setEmail("partner@tokobapak.com");
        activePartner.setActive(true);

        inactivePartner = new PartnerEntity();
        inactivePartner.setId(2L);
        inactivePartner.setName("Inactive Corp");
        inactivePartner.setType("MERCHANT");
        inactivePartner.setActive(false);

        testSubscription = new WebhookSubscriptionEntity(
                activePartner,
                "https://api.tokobapak.com/webhooks",
                "payment.completed,payment.failed",
                "test-secret-key-abc123"
        );
        testSubscription.setId(100L);
        testSubscription.setDescription("Payment events");
        testSubscription.setMaxRetries(5);
        testSubscription.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Create Subscription")
    class CreateSubscription {

        @Test
        @DisplayName("should create webhook subscription with generated secret")
        void shouldCreateSubscription() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://api.tokobapak.com/webhooks");
            dto.setEvents("payment.completed,payment.failed");
            dto.setDescription("Payment notifications");
            dto.setMaxRetries(3);

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(subscriptionRepository.existsByPartnerIdAndUrl(1L, dto.getUrl())).thenReturn(false);
            when(subscriptionRepository.save(any(WebhookSubscriptionEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookSubscriptionEntity saved = inv.getArgument(0);
                        saved.setId(100L);
                        saved.setCreatedAt(LocalDateTime.now());
                        return saved;
                    });

            WebhookSubscriptionDTO result = webhookService.createSubscription(1L, dto);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals("https://api.tokobapak.com/webhooks", result.getUrl());
            assertEquals("payment.completed,payment.failed", result.getEvents());
            assertNotNull(result.getSecret(), "Secret must be returned on creation");
            assertFalse(result.getSecret().isEmpty());
            assertEquals(3, result.getMaxRetries());

            // Verify saved entity
            ArgumentCaptor<WebhookSubscriptionEntity> captor =
                    ArgumentCaptor.forClass(WebhookSubscriptionEntity.class);
            verify(subscriptionRepository).save(captor.capture());
            WebhookSubscriptionEntity saved = captor.getValue();
            assertEquals(activePartner, saved.getPartner());
            assertTrue(saved.isActive());
        }

        @Test
        @DisplayName("should fail for inactive partner")
        void shouldFailForInactivePartner() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://api.example.com/wh");
            dto.setEvents("*");

            when(partnerRepository.findById(2L)).thenReturn(Optional.of(inactivePartner));

            assertThrows(IllegalStateException.class,
                    () -> webhookService.createSubscription(2L, dto));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fail for non-existent partner")
        void shouldFailForNonExistentPartner() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://api.example.com/wh");
            dto.setEvents("*");

            when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> webhookService.createSubscription(999L, dto));
        }

        @Test
        @DisplayName("should fail for duplicate URL")
        void shouldFailForDuplicateUrl() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://api.example.com/wh");
            dto.setEvents("*");

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(subscriptionRepository.existsByPartnerIdAndUrl(1L, dto.getUrl())).thenReturn(true);

            assertThrows(IllegalStateException.class,
                    () -> webhookService.createSubscription(1L, dto));
        }

        @Test
        @DisplayName("should reject webhook URL resolving to a non-public address (PARTNER-PROD-003)")
        void shouldRejectInternalWebhookUrl() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://169.254.169.254/latest/meta-data");
            dto.setEvents("*");

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));

            WebhookService realValidatorService = new WebhookService(
                    subscriptionRepository, deliveryRepository, partnerRepository, new WebhookUrlValidatorService());

            assertThrows(IllegalArgumentException.class,
                    () -> realValidatorService.createSubscription(1L, dto));
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should clamp maxRetries between 1 and 10")
        void shouldClampMaxRetries() {
            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setUrl("https://api.example.com/wh");
            dto.setEvents("*");
            dto.setMaxRetries(50); // Exceeds max

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(subscriptionRepository.existsByPartnerIdAndUrl(anyLong(), anyString())).thenReturn(false);
            when(subscriptionRepository.save(any(WebhookSubscriptionEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookSubscriptionEntity s = inv.getArgument(0);
                        s.setId(101L);
                        s.setCreatedAt(LocalDateTime.now());
                        return s;
                    });

            webhookService.createSubscription(1L, dto);

            ArgumentCaptor<WebhookSubscriptionEntity> captor =
                    ArgumentCaptor.forClass(WebhookSubscriptionEntity.class);
            verify(subscriptionRepository).save(captor.capture());
            assertEquals(10, captor.getValue().getMaxRetries());
        }
    }

    @Nested
    @DisplayName("Update Subscription")
    class UpdateSubscription {

        @Test
        @DisplayName("should update subscription fields")
        void shouldUpdateSubscription() {
            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setEvents("payment.completed");
            dto.setActive(false);

            WebhookSubscriptionDTO result = webhookService.updateSubscription(1L, 100L, dto);

            assertNotNull(result);
            assertEquals("payment.completed", result.getEvents());
            assertFalse(result.getActive());
            assertNull(result.getSecret(), "Secret should not be returned on update");
        }

        @Test
        @DisplayName("should fail if subscription belongs to different partner")
        void shouldFailForWrongPartner() {
            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

            WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
            dto.setEvents("*");

            assertThrows(IllegalArgumentException.class,
                    () -> webhookService.updateSubscription(999L, 100L, dto));
        }
    }

    @Nested
    @DisplayName("Delete Subscription")
    class DeleteSubscription {

        @Test
        @DisplayName("should delete subscription")
        void shouldDeleteSubscription() {
            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

            webhookService.deleteSubscription(1L, 100L);

            verify(subscriptionRepository).delete(testSubscription);
        }

        @Test
        @DisplayName("should fail if subscription not found")
        void shouldFailIfNotFound() {
            when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> webhookService.deleteSubscription(1L, 999L));
        }
    }

    @Nested
    @DisplayName("List & Get Subscriptions")
    class ListAndGet {

        @Test
        @DisplayName("should list all subscriptions for a partner")
        void shouldListSubscriptions() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(subscriptionRepository.findByPartnerId(1L)).thenReturn(List.of(testSubscription));

            List<WebhookSubscriptionDTO> result = webhookService.listSubscriptions(1L);

            assertEquals(1, result.size());
            assertEquals("https://api.tokobapak.com/webhooks", result.get(0).getUrl());
            assertNull(result.get(0).getSecret(), "Secret should not be in list response");
        }

        @Test
        @DisplayName("should get a specific subscription")
        void shouldGetSubscription() {
            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

            WebhookSubscriptionDTO result = webhookService.getSubscription(1L, 100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
        }
    }

    @Nested
    @DisplayName("Regenerate Secret")
    class RegenerateSecret {

        @Test
        @DisplayName("should regenerate HMAC secret")
        void shouldRegenerateSecret() {
            String oldSecret = testSubscription.getSecret();
            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WebhookSubscriptionDTO result = webhookService.regenerateSecret(1L, 100L);

            assertNotNull(result.getSecret(), "New secret must be returned");
            assertNotEquals(oldSecret, result.getSecret(), "New secret should differ from old");
        }
    }

    @Nested
    @DisplayName("Get Deliveries")
    class GetDeliveries {

        @Test
        @DisplayName("should return paginated delivery log")
        void shouldReturnDeliveries() {
            WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                    testSubscription, "evt_abc123", "payment.completed", "{\"test\":true}");
            delivery.setId(200L);
            delivery.markDelivered(200, "OK");

            when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
            Pageable pageable = PageRequest.of(0, 20);
            when(deliveryRepository.findBySubscriptionIdOrderByCreatedAtDesc(100L, pageable))
                    .thenReturn(new PageImpl<>(List.of(delivery)));

            Page<WebhookDeliveryDTO> result = webhookService.getDeliveries(1L, 100L, pageable);

            assertEquals(1, result.getTotalElements());
            WebhookDeliveryDTO dto = result.getContent().get(0);
            assertEquals("evt_abc123", dto.getEventId());
            assertEquals("DELIVERED", dto.getStatus());
            assertEquals(200, dto.getResponseCode());
        }
    }
}
