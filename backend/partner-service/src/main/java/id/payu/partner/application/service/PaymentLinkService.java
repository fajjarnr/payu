package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.PaymentLinkRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.PaymentLinkEntity;
import id.payu.partner.interfaces.dto.CreatePaymentLinkRequest;
import id.payu.partner.interfaces.dto.PaymentLinkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import id.payu.outbox.service.OutboxService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import id.payu.partner.domain.PaymentLinkStatus;

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
    private final OutboxService outboxService;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository,
                              PartnerRepository partnerRepository,
                              WebhookDispatcherService webhookDispatcher,
                              OutboxService outboxService) {
        this.paymentLinkRepository = paymentLinkRepository;
        this.partnerRepository = partnerRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.outboxService = outboxService;
    }

    /**
     * Create a new payment link for a partner.
     */
    public PaymentLinkResponse createPaymentLink(Long partnerId, CreatePaymentLinkRequest request) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerEntity not found: " + partnerId));

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

        PaymentLinkEntity paymentLink = new PaymentLinkEntity(
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
        PaymentLinkEntity paymentLink = paymentLinkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));

        // Auto-expire if past expiry (IMP-2: conditional transition, so a
        // concurrent confirm cannot be overwritten)
        if (paymentLink.getStatus() == PaymentLinkStatus.ACTIVE
                && paymentLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            paymentLinkRepository.markExpiredIfActive(paymentLink.getId());
            paymentLink = paymentLinkRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));
        }

        return toResponse(paymentLink);
    }

    /**
     * Get payment link by ID for a partner.
     */
    @Transactional(readOnly = true)
    public PaymentLinkResponse getByIdForPartner(Long partnerId, Long linkId) {
        PaymentLinkEntity paymentLink = paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkId));

        if (!Objects.equals(paymentLink.getPartner() != null ? paymentLink.getPartner().getId() : null, partnerId)) {
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
     *
     * IMP-2: ACTIVE → PAID is a conditional UPDATE — of two concurrent confirms
     * (or a confirm racing the expiry scheduler) exactly one wins; the loser
     * returns the existing result as a deterministic no-op.
     */
    public PaymentLinkResponse confirmPayment(String slug, String paymentMethod, String paymentReference) {
        PaymentLinkEntity paymentLink = paymentLinkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));

        int transitioned = paymentLinkRepository.markPaidIfActive(
                slug, LocalDateTime.now(), paymentMethod, paymentReference);

        if (transitioned == 0) {
            PaymentLinkEntity current = paymentLinkRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + slug));
            if (current.getStatus() == PaymentLinkStatus.PAID) {
                log.info("Double confirm for payment link {} — already paid, returning existing result", slug);
                return toResponse(current);
            }
            throw new IllegalStateException("Payment link is not active or has expired");
        }

        // Mirror the DB transition on the loaded entity (the @Modifying update already persisted it)
        paymentLink.markPaid(paymentMethod, paymentReference);

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
        PaymentLinkEntity paymentLink = paymentLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Payment link not found: " + linkId));

        if (!Objects.equals(paymentLink.getPartner() != null ? paymentLink.getPartner().getId() : null, partnerId)) {
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
    @SchedulerLock(name = "PaymentLinkService_expirePaymentLinks", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    @Scheduled(fixedRate = 300000)
    public void expirePaymentLinks() {
        try {
            List<PaymentLinkEntity> expiredLinks = paymentLinkRepository.findExpiredActiveLinks(LocalDateTime.now());
            int transitioned = 0;
            for (PaymentLinkEntity link : expiredLinks) {
                // IMP-2: conditional transition — a link confirmed concurrently is not overwritten
                if (paymentLinkRepository.markExpiredIfActive(link.getId()) > 0) {
                    transitioned++;
                    dispatchPaymentLinkExpiredEvent(link);
                }
            }
            if (transitioned > 0) {
                log.info("Expired {} payment links", transitioned);
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred while expiring payment links", e);
        }
    }

    /**
     * Dispatch webhook for payment_link.paid event.
     */
    private void dispatchPaymentLinkPaidEvent(PaymentLinkEntity paymentLink) {
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

            // MSG-009: Publish to Kafka via outbox for transactional atomicity
            outboxService.createEvent(
                    "PaymentLink",
                    paymentLink.getSlug(),
                    "PaymentLinkPaid",
                    payload,
                    null,
                    "payu.partner.payment-link-event.v1"
            );

            log.info("Dispatched payment_link.paid event for link {}", paymentLink.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch payment_link.paid event for link {}", paymentLink.getId(), e);
        }
    }

    /**
     * Dispatch webhook for payment_link.expired event.
     */
    private void dispatchPaymentLinkExpiredEvent(PaymentLinkEntity paymentLink) {
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

            // MSG-009: Publish to Kafka via outbox for transactional atomicity
            outboxService.createEvent(
                    "PaymentLink",
                    paymentLink.getSlug(),
                    "PaymentLinkExpired",
                    payload,
                    null,
                    "payu.partner.payment-link-event.v1"
            );

            log.info("Dispatched payment_link.expired event for link {}", paymentLink.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch payment_link.expired event for link {}", paymentLink.getId(), e);
        }
    }

    private PaymentLinkResponse toResponse(PaymentLinkEntity entity) {
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
