package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.MerchantQrPaymentRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.MerchantEntity;
import id.payu.partner.adapter.persistence.entity.MerchantQrPaymentEntity;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.CreateMerchantRequest;
import id.payu.partner.dto.CreateQrPaymentRequest;
import id.payu.partner.dto.MerchantResponse;
import id.payu.partner.dto.QrPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import id.payu.outbox.service.OutboxService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import id.payu.partner.domain.MerchantCategory;

/**
 * Manages merchant lifecycle and dynamic QRIS payment generation.
 * Merchants are onboarded under a partner, generate dynamic QR codes per transaction.
 */
@Service
@Transactional
// TODO BUG-ARCH-004: Migrate LocalDateTime fields to OffsetDateTime or Instant for timezone safety
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    private final MerchantRepository merchantRepository;
    private final MerchantQrPaymentRepository qrPaymentRepository;
    private final PartnerRepository partnerRepository;
    private final WebhookDispatcherService webhookDispatcher;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String WALLET_SERVICE_URL = "http://wallet-service/api/v1/wallets";

    public MerchantService(MerchantRepository merchantRepository,
                           MerchantQrPaymentRepository qrPaymentRepository,
                           PartnerRepository partnerRepository,
                           WebhookDispatcherService webhookDispatcher,
                           OutboxService outboxService,
                           ObjectMapper objectMapper) {
        this.merchantRepository = merchantRepository;
        this.qrPaymentRepository = qrPaymentRepository;
        this.partnerRepository = partnerRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        // BUG-ARCH-006 FIX: Configure RestTemplate with timeouts instead of bare new RestTemplate()
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Onboard a new merchant under a partner.
     */
    public MerchantResponse createMerchant(Long partnerId, CreateMerchantRequest request) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerEntity not found: " + partnerId));

        if (!partner.isActive()) {
            throw new IllegalStateException("Cannot create merchant for inactive partner");
        }

        MerchantCategory category;
        try {
            category = MerchantCategory.valueOf(request.getCategory());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid merchant category: " + request.getCategory());
        }

        String merchantCode = generateMerchantCode();

        MerchantEntity merchant = new MerchantEntity(partner, merchantCode, request.getBusinessName(),
                category, request.getAddress());
        merchant.setBusinessType(request.getBusinessType());
        merchant.setCity(request.getCity());
        merchant.setPostalCode(request.getPostalCode());
        merchant.setPicName(request.getPicName());
        merchant.setPicPhone(request.getPicPhone());
        merchant.setPicEmail(request.getPicEmail());
        merchant.setSettlementAccountId(request.getSettlementAccountId());
        // Generate static QR code for the merchant
        merchant.setStaticQrCode("QRIS:STATIC:" + merchantCode);

        merchant = merchantRepository.save(merchant);
        log.info("Onboarded merchant {} (code={}) for partner {}", merchant.getId(), merchantCode, partnerId);

        return toMerchantResponse(merchant);
    }

    /**
     * Get merchant by ID.
     */
    @Transactional(readOnly = true)
    public MerchantResponse getMerchant(Long merchantId) {
        MerchantEntity merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("MerchantEntity not found: " + merchantId));
        return toMerchantResponse(merchant);
    }

    /**
     * List merchants for a partner.
     */
    @Transactional(readOnly = true)
    public Page<MerchantResponse> listByPartner(Long partnerId, Pageable pageable) {
        return merchantRepository.findByPartnerId(partnerId, pageable)
                .map(this::toMerchantResponse);
    }

    /**
     * Activate a pending merchant.
     */
    public MerchantResponse activateMerchant(Long merchantId) {
        MerchantEntity merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("MerchantEntity not found: " + merchantId));
        merchant.activate();
        merchant = merchantRepository.save(merchant);
        log.info("Activated merchant {} (code={})", merchantId, merchant.getMerchantCode());
        return toMerchantResponse(merchant);
    }

    /**
     * Generate a dynamic QR code for a merchant payment.
     */
    public QrPaymentResponse generateDynamicQr(Long merchantId, CreateQrPaymentRequest request) {
        MerchantEntity merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("MerchantEntity not found: " + merchantId));

        if (!merchant.isActive()) {
            throw new IllegalStateException("MerchantEntity is not active: " + merchant.getStatus());
        }

        int expiryMinutes = request.getExpiryMinutes() != null ? request.getExpiryMinutes() : 30;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        MerchantQrPaymentEntity qrPayment = new MerchantQrPaymentEntity(
                merchant,
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "IDR",
                request.getDescription(),
                expiresAt
        );

        qrPayment = qrPaymentRepository.save(qrPayment);
        log.info("Generated dynamic QR {} for merchant {} amount={}",
                qrPayment.getReferenceId(), merchantId, request.getAmount());

        return toQrResponse(qrPayment);
    }

    /**
     * Get QR payment by reference ID.
     */
    @Transactional(readOnly = true)
    public QrPaymentResponse getQrPayment(String referenceId) {
        MerchantQrPaymentEntity qrPayment = qrPaymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("QR payment not found: " + referenceId));
        return toQrResponse(qrPayment);
    }

    /**
     * Confirm QR payment (called when payer scans and pays).
     * Triggers settlement to merchant wallet.
     */
    public QrPaymentResponse confirmQrPayment(String referenceId, String payerAccountId) {
        MerchantQrPaymentEntity qrPayment = qrPaymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("QR payment not found: " + referenceId));

        if (!qrPayment.isPending()) {
            throw new IllegalStateException("QR payment is not pending or has expired");
        }

        String paymentRef = "QRIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        qrPayment.markPaid(payerAccountId, paymentRef);
        qrPayment = qrPaymentRepository.save(qrPayment);

        log.info("QR payment {} confirmed by payer {}, ref={}",
                referenceId, payerAccountId, paymentRef);

        // Trigger settlement to merchant wallet
        settleToMerchantWallet(qrPayment);

        // Dispatch webhook notification
        dispatchQrPaymentPaidEvent(qrPayment);

        return toQrResponse(qrPayment);
    }

    /**
     * Settle payment to merchant wallet.
     * BUG-BE-184 FIX: Debits payer wallet before crediting merchant wallet
     * to prevent money creation from nothing.
     */
    private void settleToMerchantWallet(MerchantQrPaymentEntity qrPayment) {
        try {
            MerchantEntity merchant = qrPayment.getMerchant();
            String settlementAccountId = merchant.getSettlementAccountId();

            if (settlementAccountId == null || settlementAccountId.isEmpty()) {
                log.warn("MerchantEntity {} has no settlement account configured", merchant.getId());
                return;
            }

            String payerAccountId = qrPayment.getPayerAccountId();
            if (payerAccountId == null || payerAccountId.isEmpty()) {
                log.error("QR payment {} has no payer account ID", qrPayment.getReferenceId());
                publishSettlementEvent(qrPayment, merchant, "FAILED: No payer account");
                return;
            }

            // BUG-BE-184 FIX: Debit payer wallet first (source of funds)
            String debitUrl = WALLET_SERVICE_URL + "/" + payerAccountId + "/debit";
            Map<String, Object> debitRequest = new HashMap<>();
            debitRequest.put("amount", qrPayment.getAmount());
            debitRequest.put("currency", qrPayment.getCurrency());
            debitRequest.put("referenceId", qrPayment.getPaymentReference());
            debitRequest.put("description", "QR Payment to merchant: " + merchant.getMerchantCode());
            debitRequest.put("sourceType", "MERCHANT_QR_PAYMENT");
            debitRequest.put("sourceId", qrPayment.getReferenceId());

            ResponseEntity<Map> debitResponse = restTemplate.postForEntity(debitUrl, debitRequest, Map.class);

            if (!debitResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to debit payer {} for QR payment {}: HTTP {}",
                        payerAccountId, qrPayment.getReferenceId(), debitResponse.getStatusCode());
                publishSettlementEvent(qrPayment, merchant, "FAILED: Payer debit failed");
                return;
            }

            log.info("Debited payer {} for QR payment {}: amount={}",
                    payerAccountId, qrPayment.getReferenceId(), qrPayment.getAmount());

            // Credit merchant wallet
            String url = WALLET_SERVICE_URL + "/" + settlementAccountId + "/credit";
            Map<String, Object> request = new HashMap<>();
            request.put("amount", qrPayment.getAmount());
            request.put("currency", qrPayment.getCurrency());
            request.put("referenceId", qrPayment.getPaymentReference());
            request.put("description", "QR Payment settlement: " + qrPayment.getReferenceId());
            request.put("sourceType", "MERCHANT_QR_PAYMENT");
            request.put("sourceId", qrPayment.getReferenceId());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Settled QR payment {} to merchant {} wallet: amount={}",
                        qrPayment.getReferenceId(), merchant.getId(), qrPayment.getAmount());

                // Publish settlement event
                publishSettlementEvent(qrPayment, merchant, "SUCCESS");
            } else {
                log.error("Failed to settle QR payment {}: HTTP {}",
                        qrPayment.getReferenceId(), response.getStatusCode());
                publishSettlementEvent(qrPayment, merchant, "FAILED");
            }
        } catch (Exception e) {
            log.error("Error settling QR payment {} to merchant wallet",
                    qrPayment.getReferenceId(), e);
            publishSettlementEvent(qrPayment, qrPayment.getMerchant(), "ERROR: " + e.getMessage());
        }
    }

    /**
     * Dispatch webhook for QR payment paid event.
     */
    private void dispatchQrPaymentPaidEvent(MerchantQrPaymentEntity qrPayment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "qr_payment.paid");
            payload.put("referenceId", qrPayment.getReferenceId());
            payload.put("merchantId", qrPayment.getMerchant().getId());
            payload.put("merchantCode", qrPayment.getMerchant().getMerchantCode());
            payload.put("amount", qrPayment.getAmount());
            payload.put("currency", qrPayment.getCurrency());
            payload.put("status", qrPayment.getStatus().name());
            payload.put("paymentReference", qrPayment.getPaymentReference());
            payload.put("payerAccountId", qrPayment.getPayerAccountId());
            payload.put("paidAt", qrPayment.getPaidAt().toString());

            webhookDispatcher.dispatch("qr_payment.paid", payload);

            log.info("Dispatched qr_payment.paid event for {}", qrPayment.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to dispatch qr_payment.paid event for {}", qrPayment.getReferenceId(), e);
        }
    }

    /**
     * Publish settlement event to Kafka.
     */
    private void publishSettlementEvent(MerchantQrPaymentEntity qrPayment, MerchantEntity merchant, String status) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "merchant.settlement");
            event.put("referenceId", qrPayment.getReferenceId());
            event.put("paymentReference", qrPayment.getPaymentReference());
            event.put("merchantId", merchant.getId());
            event.put("merchantCode", merchant.getMerchantCode());
            event.put("settlementAccountId", merchant.getSettlementAccountId());
            event.put("amount", qrPayment.getAmount());
            event.put("currency", qrPayment.getCurrency());
            event.put("status", status);
            event.put("settledAt", LocalDateTime.now().toString());

            // MSG-009: Publish via outbox for transactional atomicity
            outboxService.createEvent(
                    "MerchantSettlement",
                    qrPayment.getReferenceId(),
                    "MerchantSettlement",
                    event,
                    null,
                    "payu.partner.merchant-settlement.v1"
            );

            log.info("Published merchant.settlement event for {}", qrPayment.getReferenceId());
        } catch (Exception e) {
            log.error("Failed to publish settlement event for {}", qrPayment.getReferenceId(), e);
        }
    }

    private String mapToJson(Map<String, Object> map) {
        // BUG-LOGIC-004 FIX: Use Jackson ObjectMapper instead of manual StringBuilder
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON", e);
            return "{}";
        }
    }

    /**
     * Scheduled job to expire pending QR payments past their expiry.
     * Runs every 2 minutes.
     */
    @SchedulerLock(name = "MerchantService_expireQrPayments", lockAtLeastFor = "PT1S", lockAtMostFor = "PT2M")@Scheduled(fixedRate = 120000)
    public void expireQrPayments() {
        List<MerchantQrPaymentEntity> expired = qrPaymentRepository.findExpiredPendingPayments(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(MerchantQrPaymentEntity::markExpired);
            qrPaymentRepository.saveAll(expired);
            log.info("Expired {} QR payments", expired.size());
        }
    }

    private String generateMerchantCode() {
        String code;
        do {
            code = "MCH" + UUID.randomUUID().toString().substring(0, 10).toUpperCase().replace("-", "");
        } while (merchantRepository.existsByMerchantCode(code));
        return code;
    }

    private MerchantResponse toMerchantResponse(MerchantEntity entity) {
        MerchantResponse response = new MerchantResponse();
        response.setId(entity.getId());
        response.setMerchantCode(entity.getMerchantCode());
        response.setBusinessName(entity.getBusinessName());
        response.setBusinessType(entity.getBusinessType());
        response.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        response.setAddress(entity.getAddress());
        response.setCity(entity.getCity());
        response.setPostalCode(entity.getPostalCode());
        response.setPicName(entity.getPicName());
        response.setPicPhone(entity.getPicPhone());
        response.setPicEmail(entity.getPicEmail());
        response.setSettlementAccountId(entity.getSettlementAccountId());
        response.setStatus(entity.getStatus().name());
        response.setStaticQrCode(entity.getStaticQrCode());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private QrPaymentResponse toQrResponse(MerchantQrPaymentEntity entity) {
        QrPaymentResponse response = new QrPaymentResponse();
        response.setId(entity.getId());
        response.setReferenceId(entity.getReferenceId());
        response.setMerchantId(entity.getMerchant().getId());
        response.setMerchantName(entity.getMerchant().getBusinessName());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setDescription(entity.getDescription());
        response.setQrContent(entity.getQrContent());
        response.setStatus(entity.getStatus().name());
        response.setPayerAccountId(entity.getPayerAccountId());
        response.setPaymentReference(entity.getPaymentReference());
        response.setPaidAt(entity.getPaidAt());
        response.setExpiresAt(entity.getExpiresAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
