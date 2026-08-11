package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.WebhookDeliveryRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookDeliveryEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.dto.WebhookDeliveryDTO;
import id.payu.partner.dto.WebhookSubscriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Manages webhook subscription CRUD operations.
 * Partners register URLs to receive event notifications with HMAC signature verification.
 */
@Service
@Transactional
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final PartnerRepository partnerRepository;
    private final WebhookUrlValidatorService webhookUrlValidator;

    public WebhookService(WebhookSubscriptionRepository subscriptionRepository,
                          WebhookDeliveryRepository deliveryRepository,
                          PartnerRepository partnerRepository,
                          WebhookUrlValidatorService webhookUrlValidator) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.partnerRepository = partnerRepository;
        this.webhookUrlValidator = webhookUrlValidator;
    }

    /**
     * Register a new webhook subscription for a partner.
     * Generates HMAC secret and returns it once (not stored in response after creation).
     */
    public WebhookSubscriptionDTO createSubscription(Long partnerId, WebhookSubscriptionDTO dto) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerEntity not found: " + partnerId));

        if (!partner.isActive()) {
            throw new IllegalStateException("Cannot create webhook for inactive partner");
        }

        // PARTNER-PROD-003: trust-boundary validation before persistence
        webhookUrlValidator.validate(dto.getUrl());

        if (subscriptionRepository.existsByPartnerIdAndUrl(partnerId, dto.getUrl())) {
            throw new IllegalStateException("Webhook subscription already exists for this URL");
        }

        String secret = generateSecret();

        WebhookSubscriptionEntity subscription = new WebhookSubscriptionEntity(
                partner, dto.getUrl(), dto.getEvents(), secret);
        subscription.setDescription(dto.getDescription());
        if (dto.getMaxRetries() != null) {
            int retries = Math.min(Math.max(dto.getMaxRetries(), 1), 10);
            subscription.setMaxRetries(retries);
        }

        subscription = subscriptionRepository.save(subscription);
        log.info("Created webhook subscription {} for partner {} -> {}",
                subscription.getId(), partnerId, dto.getUrl());

        // Return DTO with secret visible (only shown at creation)
        return toDTO(subscription, true);
    }

    /**
     * Update an existing webhook subscription.
     */
    public WebhookSubscriptionDTO updateSubscription(Long partnerId, Long subscriptionId,
                                                     WebhookSubscriptionDTO dto) {
        WebhookSubscriptionEntity subscription = findSubscriptionForPartner(partnerId, subscriptionId);

        if (dto.getUrl() != null) {
            // PARTNER-PROD-003: trust-boundary validation on URL change
            webhookUrlValidator.validate(dto.getUrl());
            subscription.setUrl(dto.getUrl());
        }
        if (dto.getEvents() != null) {
            subscription.setEvents(dto.getEvents());
        }
        if (dto.getDescription() != null) {
            subscription.setDescription(dto.getDescription());
        }
        if (dto.getActive() != null) {
            subscription.setActive(dto.getActive());
        }
        if (dto.getMaxRetries() != null) {
            int retries = Math.min(Math.max(dto.getMaxRetries(), 1), 10);
            subscription.setMaxRetries(retries);
        }

        subscription = subscriptionRepository.save(subscription);
        log.info("Updated webhook subscription {} for partner {}", subscriptionId, partnerId);
        return toDTO(subscription, false);
    }

    /**
     * Delete a webhook subscription.
     */
    public void deleteSubscription(Long partnerId, Long subscriptionId) {
        WebhookSubscriptionEntity subscription = findSubscriptionForPartner(partnerId, subscriptionId);
        subscriptionRepository.delete(subscription);
        log.info("Deleted webhook subscription {} for partner {}", subscriptionId, partnerId);
    }

    /**
     * Get a specific webhook subscription.
     */
    @Transactional(readOnly = true)
    public WebhookSubscriptionDTO getSubscription(Long partnerId, Long subscriptionId) {
        WebhookSubscriptionEntity subscription = findSubscriptionForPartner(partnerId, subscriptionId);
        return toDTO(subscription, false);
    }

    /**
     * List all webhook subscriptions for a partner.
     */
    @Transactional(readOnly = true)
    public List<WebhookSubscriptionDTO> listSubscriptions(Long partnerId) {
        partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerEntity not found: " + partnerId));
        return subscriptionRepository.findByPartnerId(partnerId)
                .stream()
                .map(s -> toDTO(s, false))
                .toList();
    }

    /**
     * Regenerate the HMAC secret for a subscription.
     * Returns the new secret (shown only once).
     */
    public WebhookSubscriptionDTO regenerateSecret(Long partnerId, Long subscriptionId) {
        WebhookSubscriptionEntity subscription = findSubscriptionForPartner(partnerId, subscriptionId);
        String newSecret = generateSecret();
        subscription.setSecret(newSecret);
        subscription = subscriptionRepository.save(subscription);
        log.info("Regenerated webhook secret for subscription {} partner {}", subscriptionId, partnerId);
        return toDTO(subscription, true);
    }

    /**
     * Get delivery log for a subscription.
     */
    @Transactional(readOnly = true)
    public Page<WebhookDeliveryDTO> getDeliveries(Long partnerId, Long subscriptionId, Pageable pageable) {
        // Verify ownership
        findSubscriptionForPartner(partnerId, subscriptionId);
        return deliveryRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId, pageable)
                .map(this::toDeliveryDTO);
    }

    // --- Internal helpers ---

    private WebhookSubscriptionEntity findSubscriptionForPartner(Long partnerId, Long subscriptionId) {
        WebhookSubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Webhook subscription not found: " + subscriptionId));

        if (!Objects.equals(subscription.getPartner() != null ? subscription.getPartner().getId() : null, partnerId)) {
            throw new IllegalArgumentException(
                    "Webhook subscription " + subscriptionId + " does not belong to partner " + partnerId);
        }
        return subscription;
    }

    /**
     * Generate a 32-byte cryptographic secret for HMAC signing, Base64url encoded.
     */
    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private WebhookSubscriptionDTO toDTO(WebhookSubscriptionEntity entity, boolean includeSecret) {
        WebhookSubscriptionDTO dto = new WebhookSubscriptionDTO();
        dto.setId(entity.getId());
        dto.setUrl(entity.getUrl());
        dto.setEvents(entity.getEvents());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        dto.setMaxRetries(entity.getMaxRetries());
        if (includeSecret) {
            dto.setSecret(entity.getSecret());
        }
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(FORMATTER));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(FORMATTER));
        }
        return dto;
    }

    private WebhookDeliveryDTO toDeliveryDTO(WebhookDeliveryEntity entity) {
        WebhookDeliveryDTO dto = new WebhookDeliveryDTO();
        dto.setId(entity.getId());
        dto.setEventId(entity.getEventId());
        dto.setEventType(entity.getEventType());
        dto.setStatus(entity.getStatus().name());
        dto.setAttemptCount(entity.getAttemptCount());
        dto.setMaxAttempts(entity.getMaxAttempts());
        dto.setResponseCode(entity.getResponseCode());
        dto.setErrorMessage(entity.getErrorMessage());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(FORMATTER));
        }
        if (entity.getLastAttemptAt() != null) {
            dto.setLastAttemptAt(entity.getLastAttemptAt().format(FORMATTER));
        }
        if (entity.getNextRetryAt() != null) {
            dto.setNextRetryAt(entity.getNextRetryAt().format(FORMATTER));
        }
        if (entity.getDeliveredAt() != null) {
            dto.setDeliveredAt(entity.getDeliveredAt().format(FORMATTER));
        }
        return dto;
    }
}
