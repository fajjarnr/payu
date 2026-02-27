package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.repository.VirtualAccountRepository;
import id.payu.transaction.domain.model.VirtualAccount;
import id.payu.transaction.dto.CreateVirtualAccountRequest;
import id.payu.transaction.dto.VaCallbackRequest;
import id.payu.transaction.dto.VirtualAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages Virtual Account lifecycle: creation, callback handling, and auto-expiry.
 * Generates VA numbers for BCA, BNI, Mandiri, Permata banks.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VirtualAccountService {

    private final VirtualAccountRepository virtualAccountRepository;

    /**
     * Create a new Virtual Account with a generated VA number.
     */
    public VirtualAccountResponse createVirtualAccount(CreateVirtualAccountRequest request) {
        VirtualAccount.BankCode bank = VirtualAccount.BankCode.fromCode(request.getBankCode());

        String vaNumber = generateVaNumber(bank);

        int expiryHours = request.getExpiryHours() != null ? request.getExpiryHours() : 24;

        VirtualAccount va = VirtualAccount.builder()
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
                .status(VirtualAccount.VaStatus.PENDING)
                .expiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
                .build();

        va = virtualAccountRepository.save(va);
        log.info("Created VA {} (bank={}, number={}) for partner {}",
                va.getId(), bank.name(), vaNumber, request.getPartnerId());

        return toResponse(va);
    }

    /**
     * Get VA details by ID.
     */
    @Transactional(readOnly = true)
    public VirtualAccountResponse getById(UUID vaId) {
        VirtualAccount va = virtualAccountRepository.findById(vaId)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + vaId));
        return toResponse(va);
    }

    /**
     * Get VA details by VA number.
     */
    @Transactional(readOnly = true)
    public VirtualAccountResponse getByVaNumber(String vaNumber) {
        VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                .orElseThrow(() -> new IllegalArgumentException("Virtual account not found: " + vaNumber));
        return toResponse(va);
    }

    /**
     * Handle bank callback confirming VA payment.
     * Called when bank confirms customer has paid to the VA number.
     */
    public VirtualAccountResponse handleBankCallback(VaCallbackRequest callback) {
        VirtualAccount va = virtualAccountRepository.findByVaNumber(callback.getVaNumber())
                .orElseThrow(() -> new IllegalArgumentException("VA not found: " + callback.getVaNumber()));

        if (!va.isPending()) {
            throw new IllegalStateException("VA is not pending or has expired: " + va.getStatus());
        }

        va.markPaid(callback.getAmount(), callback.getPaymentReference());
        va = virtualAccountRepository.save(va);

        log.info("VA {} paid: amount={}, ref={}", va.getVaNumber(),
                callback.getAmount(), callback.getPaymentReference());

        return toResponse(va);
    }

    /**
     * Expire unpaid VAs past their TTL.
     * Called by PaymentExpiryScheduler (not scheduled here to avoid duplication).
     */
    public void expireVirtualAccounts() {
        List<VirtualAccount> expired = virtualAccountRepository.findExpiredPendingVAs(Instant.now());
        if (!expired.isEmpty()) {
            expired.forEach(VirtualAccount::markExpired);
            virtualAccountRepository.saveAll(expired);
            log.info("Expired {} virtual accounts", expired.size());
        }
    }

    private String generateVaNumber(VirtualAccount.BankCode bank) {
        String vaNumber;
        do {
            // Format: bankPrefix + 12 random digits
            long random = ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L);
            vaNumber = bank.getPrefix() + random;
        } while (virtualAccountRepository.existsByVaNumber(vaNumber));
        return vaNumber;
    }

    private VirtualAccountResponse toResponse(VirtualAccount va) {
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
