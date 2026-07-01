package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.VirtualAccountEntity;
import id.payu.transaction.domain.port.out.VirtualAccountPersistencePort;
import id.payu.transaction.dto.CreateVirtualAccountRequest;
import id.payu.transaction.dto.VaCallbackRequest;
import id.payu.transaction.dto.VirtualAccountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import id.payu.transaction.domain.model.BankCode;
import id.payu.transaction.domain.model.VaStatus;

/**
 * Manages Virtual Account lifecycle: creation, callback handling, and auto-expiry.
 * Generates VA numbers for BCA, BNI, Mandiri, Permata banks.
 */
@Service
@Transactional
public class VirtualAccountService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VirtualAccountService.class);



    private final VirtualAccountPersistencePort virtualAccountPersistencePort;

    public VirtualAccountService(VirtualAccountPersistencePort virtualAccountPersistencePort) {
        this.virtualAccountPersistencePort = virtualAccountPersistencePort;
    }

    /**
     * Create a new Virtual Account with a generated VA number.
     */
    public VirtualAccountResponse createVirtualAccount(CreateVirtualAccountRequest request) {
        BankCode bank = BankCode.fromCode(request.getBankCode());

        String vaNumber = generateVaNumber(bank);

        int expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : 24;

        VirtualAccountEntity va = VirtualAccountEntity.builder()
                .vaNumber(vaNumber)
                .bankCode(bank.name())
                .bankName(bank.getBankName())
                .partnerId(request.getPartnerId())
                .externalId(request.getExternalId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .description(request.getDescription())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .callbackUrl(request.getCallbackUrl())
                .status(VaStatus.PENDING)
                .expiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
                .build();

        va = virtualAccountPersistencePort.save(va);
        log.info("Created VA {} (bank={}, number={}) for partner {}",
                va.getId(), bank.name(), vaNumber, request.getPartnerId());

        return toResponse(va);
    }

    /**
     * Get VA details by ID.
     */
    @Transactional(readOnly = true)
    public VirtualAccountResponse getById(UUID vaId) {
        VirtualAccountEntity va = virtualAccountPersistencePort.findById(vaId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + vaId));
        return toResponse(va);
    }

    /**
     * Get VA details by VA number.
     */
    @Transactional(readOnly = true)
    public VirtualAccountResponse getByVaNumber(String vaNumber) {
        VirtualAccountEntity va = virtualAccountPersistencePort.findByVaNumber(vaNumber)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + vaNumber));
        return toResponse(va);
    }

    /**
     * Handle bank callback confirming VA payment.
     * Called when bank confirms customer has paid to the VA number.
     */
    public VirtualAccountResponse handleBankCallback(VaCallbackRequest callback) {
        VirtualAccountEntity va = virtualAccountPersistencePort.findByVaNumber(callback.getVaNumber())
                .orElseThrow(() -> new IllegalArgumentException("VA not found: " + callback.getVaNumber()));

        if (!va.isPending()) {
            throw new IllegalStateException("VA is not pending or has expired: " + va.getStatus());
        }

        // Validate callback amount matches VA expected amount (if fixed amount VA)
        if (va.getAmount() != null && callback.getAmount() != null
                && va.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                && callback.getAmount().compareTo(va.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Callback amount " + callback.getAmount() + " does not match VA expected amount " + va.getAmount());
        }

        va.markPaid(callback.getAmount(), callback.getPaymentReference());
        va = virtualAccountPersistencePort.save(va);

        log.info("VA {} paid: amount={}, ref={}", va.getVaNumber(),
                callback.getAmount(), callback.getPaymentReference());

        return toResponse(va);
    }

    /**
     * Expire unpaid VAs past their TTL.
     * Called by PaymentExpiryScheduler (not scheduled here to avoid duplication).
     */
    public void expireVirtualAccounts() {
        List<VirtualAccountEntity> expired = virtualAccountPersistencePort.findExpiredPendingVAs(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(VirtualAccountEntity::markExpired);
            virtualAccountPersistencePort.saveAll(expired);
            log.info("Expired {} virtual accounts", expired.size());
        }
    }

    private String generateVaNumber(BankCode bank) {
        String vaNumber;
        do {
            // Format: bankPrefix + 12 random digits
            long random = ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L);
            vaNumber = bank.getPrefix() + random;
        } while (virtualAccountPersistencePort.existsByVaNumber(vaNumber));
        return vaNumber;
    }

    private VirtualAccountResponse toResponse(VirtualAccountEntity va) {
        return VirtualAccountResponse.builder()
                .id(va.getId())
                .vaNumber(va.getVaNumber())
                .bankCode(va.getBankCode())
                .bankName(va.getBankName())
                .partnerId(va.getPartnerId())
                .externalId(va.getExternalId())
                .amount(va.getAmount())
                .currency(va.getCurrency())
                .description(va.getDescription())
                .customerName(va.getCustomerName())
                .status(va.getStatus().name())
                .paidAmount(va.getPaidAmount())
                .paidAt(va.getPaidAt())
                .paymentReference(va.getPaymentReference())
                .expiresAt(va.getExpiresAt())
                .createdAt(va.getCreatedAt())
                .build();
    }
}
