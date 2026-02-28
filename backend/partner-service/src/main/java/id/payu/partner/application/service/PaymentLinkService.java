package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.PaymentLinkRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.Partner;
import id.payu.partner.domain.PaymentLink;
import id.payu.partner.dto.CreatePaymentLinkRequest;
import id.payu.partner.dto.PaymentLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages payment link lifecycle: creation, retrieval, payment confirmation, and expiry.
 * Partners generate shareable URLs with amount, description, and expiry.
 */
@Service
@Transactional
public class PaymentLinkService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLinkService.class);
    private static final String BASE_PAYMENT_URL = "https://pay.payu.fajjjar.my.id/pay/";

    private final PaymentLinkRepository paymentLinkRepository;
    private final PartnerRepository partnerRepository;
    private final WebhookDispatcherService webhookDispatcher;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository,
                              PartnerRepository partnerRepository,
                              WebhookDispatcherService webhookDispatcher,
                              KafkaTemplate<String, String> kafkaTemplate) {
        this.paymentLinkRepository = paymentLinkRepository;
        this.partnerRepository = partnerRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create a new payment link for a partner.
     */
    public PaymentLinkResponse createPaymentLink(Long partnerId, CreatePaymentLinkRequest request) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!partner.isActive()) {
            throw new IllegalStateException("Cannot create payment link for inactive partner");
        }

        // Check duplicate external ID
        if (request.getExternalId() != null &&
                paymentLinkRepository.existsByPartnerIdAndExternalId(partnerId, request.getExternalId())) {
            throw new IllegalStateException("Payment link with external ID already exists: " + request.getExternalId());
        }

        int expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : 24;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

        PaymentLink paymentLink = new PaymentLink(
                partner,
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "IDR",
                request.getDescription(),
                expiresAt
        );
        paymentLink.setCustomerName(request.getCustomerName());
        paymentLink.setCustomerEmail(request.getCustomerEmail());
        paymentLink.setExternalId(request.getExternalId());
        paymentLink.setCallbackUrl(request.getCallbackUrl());
        paymentLink.setRedirectUrl(request.getRedirectUrl());

        paymentLink = paymentLinkRepository.save(paymentLink);
        log.info("Created payment link {} (slug={}) for partner {}",
                paymentLink.getId(), paymentLink.getSlug(), partnerId);

        return toResponse(paymentLink);
    }

    /**
     * Get payment link details by slug (public endpoint for payer).
     * Note: Not readOnly because auto-expire may write.
     */
    public PaymentLinkResponse getBySlug(String slug) {
        PaymentLink paymentLink = paymentLinkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));

        // Auto-expire if past expiry
        if (paymentLink.getStatus() == PaymentLink.PaymentLinkStatus.ACTIVE
                && paymentLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            paymentLink.markExpired();
            paymentLinkRepository.save(paymentLink);
        }

        return toResponse(paymentLink);
    }

    /**
     * Get payment link by ID for a partner.
     */
    @Transactional(readOnly = true)
    public PaymentLinkResponse getByIdForPartner(Long partnerId, Long linkId) {
        PaymentLink paymentLink = paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkId));

        if (!paymentLink.getPartner().getId().equals(partnerId)) {
            throw new IllegalArgumentException("Payment link does not belong to partner: " + partnerId);
        }

        return toResponse(paymentLink);
    }

    /**
     * List all payment links for a partner.
     */
    @Transactional(readOnly = true)
    public Page<PaymentLinkResponse> listByPartner(Long partnerId, Pageable pageable) {
        return paymentLinkRepository.findByPartnerId(partnerId, pageable)
                .map(this::toResponse);
    }

    /**
     * Mark a payment link as paid (called when payment is confirmed).
     * Dispatches webhook notification on payment completion.
     */
    public PaymentLinkResponse confirmPayment(String slug, String paymentMethod, String paymentReference) {
        PaymentLink paymentLink = paymentLinkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));

        if (!paymentLink.isActive()) {
            throw new IllegalStateException("Payment link is not active or has expired");
        }

        paymentLink.markPaid(paymentMethod, paymentReference);
        paymentLink = paymentLinkRepository.save(paymentLink);

        log.info("Payment link {} (slug={}) paid via {} ref={}",
                paymentLink.getId(), slug, paymentMethod, paymentReference);

        // Dispatch webhook notification
        dispatchPaymentLinkPaidEvent(paymentLink);

        return toResponse(paymentLink);
    }

    /**
     * Cancel a payment link.
     */
    public void cancelPaymentLink(Long partnerId, Long linkId) {
        PaymentLink paymentLink = paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkId));

        if (!paymentLink.getPartner().getId().equals(partnerId)) {
            throw new IllegalArgumentException("Payment link does not belong to partner: " + partnerId);
        }

        paymentLink.cancel();
        paymentLinkRepository.save(paymentLink);
        log.info("Cancelled payment link {} for partner {}", linkId, partnerId);
    }

    /**
     * Scheduled job to expire payment links past their expiry time.
     * Runs every 5 minutes.
     * Dispatches webhook notification for each expired link.
     */
    @Scheduled(fixedRate = 300000)
    public void expirePaymentLinks() {
        List<PaymentLink> expiredLinks = paymentLinkRepository.findExpiredActiveLinks(LocalDateTime.now());
        if (!expiredLinks.isEmpty()) {
            expiredLinks.forEach(link -> {
                link.markExpired();
                dispatchPaymentLinkExpiredEvent(link);
            });
            paymentLinkRepository.saveAll(expiredLinks);
            log.info("Expired {} payment links", expiredLinks.size());
        }
    }

    /**
     * Dispatch webhook for payment_link.paid event.
     */
    private void dispatchPaymentLinkPaidEvent(PaymentLink paymentLink) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment_link.paid");
            payload.put("linkId", paymentLink.getId());
            payload.put("slug", paymentLink.getSlug());
            payload.put("externalId", paymentLink.getExternalId());
            payload.put("amount", paymentLink.getAmount());
            payload.put("currency", paymentLink.getCurrency());
            payload.put("status", paymentLink.getStatus().name());
            payload.put("paymentMethod", paymentLink.getPaymentMethod());
            payload.put("paymentReference", paymentLink.getPaymentReference());
            payload.put("paidAt", paymentLink.getPaidAt().toString());
            payload.put("partnerId", paymentLink.getPartner().getId());

            webhookDispatcher.dispatch("payment_link.paid", payload);

            // Also publish to Kafka for other services
            kafkaTemplate.send("payment.link.events",
                paymentLink.getSlug(),
                "{\"type\":\"payment_link.paid\",\"slug\":\"" + paymentLink.getSlug() + "\"}");

            log.info("Dispatched payment_link.paid event for link {}", paymentLink.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch payment_link.paid event for link {}", paymentLink.getId(), e);
        }
    }

    /**
     * Dispatch webhook for payment_link.expired event.
     */
    private void dispatchPaymentLinkExpiredEvent(PaymentLink paymentLink) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "payment_link.expired");
            payload.put("linkId", paymentLink.getId());
            payload.put("slug", paymentLink.getSlug());
            payload.put("externalId", paymentLink.getExternalId());
            payload.put("amount", paymentLink.getAmount());
            payload.put("currency", paymentLink.getCurrency());
            payload.put("status", paymentLink.getStatus().name());
            payload.put("expiredAt", LocalDateTime.now().toString());
            payload.put("partnerId", paymentLink.getPartner().getId());

            webhookDispatcher.dispatch("payment_link.expired", payload);

            // Also publish to Kafka for other services
            kafkaTemplate.send("payment.link.events",
                paymentLink.getSlug(),
                "{\"type\":\"payment_link.expired\",\"slug\":\"" + paymentLink.getSlug() + "\"}");

            log.info("Dispatched payment_link.expired event for link {}", paymentLink.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch payment_link.expired event for link {}", paymentLink.getId(), e);
        }
    }

    private PaymentLinkResponse toResponse(PaymentLink entity) {
        PaymentLinkResponse response = new PaymentLinkResponse();
        response.setId(entity.getId());
        response.setSlug(entity.getSlug());
        response.setPaymentUrl(BASE_PAYMENT_URL + entity.getSlug());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus().name());
        response.setCustomerName(entity.getCustomerName());
        response.setCustomerEmail(entity.getCustomerEmail());
        response.setExternalId(entity.getExternalId());
        response.setRedirectUrl(entity.getRedirectUrl());
        response.setPaymentMethod(entity.getPaymentMethod());
        response.setPaymentReference(entity.getPaymentReference());
        response.setPaidAt(entity.getPaidAt());
        response.setExpiresAt(entity.getExpiresAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
