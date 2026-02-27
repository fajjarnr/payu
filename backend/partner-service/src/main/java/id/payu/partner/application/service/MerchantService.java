package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.MerchantQrPaymentRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.Merchant;
import id.payu.partner.domain.MerchantQrPayment;
import id.payu.partner.domain.Partner;
import id.payu.partner.dto.CreateMerchantRequest;
import id.payu.partner.dto.CreateQrPaymentRequest;
import id.payu.partner.dto.MerchantResponse;
import id.payu.partner.dto.QrPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages merchant lifecycle and dynamic QRIS payment generation.
 * Merchants are onboarded under a partner, generate dynamic QR codes per transaction.
 */
@Service
@Transactional
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    private final MerchantRepository merchantRepository;
    private final MerchantQrPaymentRepository qrPaymentRepository;
    private final PartnerRepository partnerRepository;

    public MerchantService(MerchantRepository merchantRepository,
                           MerchantQrPaymentRepository qrPaymentRepository,
                           PartnerRepository partnerRepository) {
        this.merchantRepository = merchantRepository;
        this.qrPaymentRepository = qrPaymentRepository;
        this.partnerRepository = partnerRepository;
    }

    /**
     * Onboard a new merchant under a partner.
     */
    public MerchantResponse createMerchant(Long partnerId, CreateMerchantRequest request) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!partner.isActive()) {
            throw new IllegalStateException("Cannot create merchant for inactive partner");
        }

        Merchant.MerchantCategory category;
        try {
            category = Merchant.MerchantCategory.valueOf(request.getCategory());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid merchant category: " + request.getCategory());
        }

        String merchantCode = generateMerchantCode();

        Merchant merchant = new Merchant(partner, merchantCode, request.getBusinessName(),
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
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));
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
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));
        merchant.activate();
        merchant = merchantRepository.save(merchant);
        log.info("Activated merchant {} (code={})", merchantId, merchant.getMerchantCode());
        return toMerchantResponse(merchant);
    }

    /**
     * Generate a dynamic QR code for a merchant payment.
     */
    public QrPaymentResponse generateDynamicQr(Long merchantId, CreateQrPaymentRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        if (!merchant.isActive()) {
            throw new IllegalStateException("Merchant is not active: " + merchant.getStatus());
        }

        int expiryMinutes = request.getExpiryMinutes() != null ? request.getExpiryMinutes() : 30;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        MerchantQrPayment qrPayment = new MerchantQrPayment(
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
        MerchantQrPayment qrPayment = qrPaymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("QR payment not found: " + referenceId));
        return toQrResponse(qrPayment);
    }

    /**
     * Confirm QR payment (called when payer scans and pays).
     */
    public QrPaymentResponse confirmQrPayment(String referenceId, String payerAccountId) {
        MerchantQrPayment qrPayment = qrPaymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("QR payment not found: " + referenceId));

        if (!qrPayment.isPending()) {
            throw new IllegalStateException("QR payment is not pending or has expired");
        }

        String paymentRef = "QRIS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        qrPayment.markPaid(payerAccountId, paymentRef);
        qrPayment = qrPaymentRepository.save(qrPayment);

        log.info("QR payment {} confirmed by payer {}, ref={}",
                referenceId, payerAccountId, paymentRef);

        return toQrResponse(qrPayment);
    }

    /**
     * Scheduled job to expire pending QR payments past their expiry.
     * Runs every 2 minutes.
     */
    @Scheduled(fixedRate = 120000)
    public void expireQrPayments() {
        List<MerchantQrPayment> expired = qrPaymentRepository.findExpiredPendingPayments(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(MerchantQrPayment::markExpired);
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

    private MerchantResponse toMerchantResponse(Merchant entity) {
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

    private QrPaymentResponse toQrResponse(MerchantQrPayment entity) {
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
