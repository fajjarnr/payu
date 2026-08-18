package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.DisbursementStatus;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.DisbursementUseCase;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.interfaces.dto.BifastTransferRequest;
import id.payu.transaction.interfaces.dto.ReserveBalanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing disbursements (payouts).
 *
 * <p>This service orchestrates the disbursement lifecycle including:
 * <ul>
 *   <li>Creation with idempotency protection</li>
 *   <li>Wallet balance reservation</li>
 *   <li>BI-FAST transfer initiation</li>
 *   <li>Callback handling for success/failure</li>
 *   <li>Balance commitment/release</li>
 * </ul>
 *
 * <p>The service follows the application service pattern from DDD, coordinating
 * between domain aggregates and external services while maintaining transaction boundaries.
 *
 * @see DisbursementEntity
 * @see DisbursementUseCase
 */
@Service
@Transactional(readOnly = true)
public class DisbursementService implements DisbursementUseCase {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DisbursementService.class);



    private final DisbursementRepositoryPort disbursementRepository;
    private final WalletServicePort walletService;
    private final BifastServicePort bifastService;

    @Value("${payu.disbursement.callback-url:}")
    private String disbursementCallbackUrl;

    public DisbursementService(DisbursementRepositoryPort disbursementRepository,
                               WalletServicePort walletService,
                               BifastServicePort bifastService) {
        this.disbursementRepository = disbursementRepository;
        this.walletService = walletService;
        this.bifastService = bifastService;
    }

    @Override
    @Transactional
    public DisbursementEntity createDisbursement(
            UUID sourceAccountId,
            Money amount,
            String bankCode,
            String accountNumber,
            String accountName,
            String description,
            String idempotencyKey) {

        log.info("Creating disbursement for account: {}, amount: {}, bank: {}",
                sourceAccountId, amount, bankCode);

        // Check idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<DisbursementEntity> existing = disbursementRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning existing disbursement for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // Create disbursement
        DisbursementEntity disbursement = DisbursementEntity.createWithIdempotencyKey(
                sourceAccountId,
                amount,
                bankCode,
                accountNumber,
                accountName,
                idempotencyKey != null && !idempotencyKey.isBlank()
                        ? idempotencyKey
                        : generateIdempotencyKey()
        );

        if (description != null && !description.isBlank()) {
            disbursement.setDescription(description);
        }

        // Reserve balance from wallet
        ReserveBalanceResponse reservation = walletService.reserveBalance(
                sourceAccountId,
                disbursement.getId().toString(),
                amount.getAmount()
        );

        log.info("Reserved balance for disbursement: {}, reservationId: {}",
                disbursement.getId(), reservation.getReservationId());
        disbursement.setReservationId(reservation.getReservationId());

        // Save disbursement using EntityManager.persist() directly to bypass
        // Spring Data JPA's isNew() detection. The default detection sees
        // (id=non-null, version=null) as "detached" and calls merge() which
        // throws StaleObjectStateException for new rows. See context7
        // spring-projects spring-data-jpa entity state-detection strategy.
        DisbursementEntity saved;
        try {
            saved = disbursementRepository.persistNew(disbursement);
            log.info("Created disbursement: {}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to persist disbursement {}, releasing reservation {}",
                    disbursement.getId(), reservation.getReservationId(), e);
            try {
                walletService.releaseBalance(sourceAccountId, disbursement.getId().toString(),
                        reservation.getReservationId(), amount.getAmount());
            } catch (Exception compEx) {
                log.error("Failed to compensate balance reservation {}: {}",
                        reservation.getReservationId(), compEx.getMessage());
            }
            throw e;
        }

        return saved;
    }

    @Override
    public Optional<DisbursementEntity> getDisbursement(UUID id) {
        return disbursementRepository.findById(id);
    }

    @Override
    public Optional<DisbursementEntity> findByIdempotencyKey(String idempotencyKey) {
        return disbursementRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<DisbursementEntity> listDisbursementsByAccount(UUID sourceAccountId, int limit, int offset) {
        return disbursementRepository.findBySourceAccountId(sourceAccountId, limit, offset);
    }
    @Override
    @Transactional
    public DisbursementEntity processDisbursement(UUID id) {
        log.info("Processing disbursement: {}", id);

        DisbursementEntity disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity not found: " + id));

        // Idempotent: only transition PENDING -> PROCESSING. If already PROCESSING/COMPLETED,
        // skip the transition (avoids IllegalStateException on retry path).
        if (disbursement.getStatus() == DisbursementStatus.PENDING) {
            disbursement.process();
        } else {
            log.info("Disbursement {} already in status {}, skipping transition", id, disbursement.getStatus());
        }

        // Initiate BI-FAST transfer
        BifastTransferRequest request = BifastTransferRequest.builder()
                .referenceNumber(disbursement.getId().toString())
                .beneficiaryBankCode(disbursement.getBankCode())
                .beneficiaryAccountNumber(disbursement.getAccountNumber())
                .beneficiaryAccountName(disbursement.getAccountName())
                .amount(disbursement.getAmount().getAmount())
                .currency(disbursement.getAmount().getCurrency().getCurrencyCode())
                .senderAccountNumber("PAYU" + disbursement.getSourceAccountId().toString().substring(0, 8))
                .senderAccountName("PayU DisbursementEntity")
                .purposeCode("PAY")
                .webhookUrl(disbursementCallbackUrl)
                .build();

        try {
            bifastService.initiateTransfer(request);
            log.info("BI-FAST transfer initiated for disbursement: {}", id);
        } catch (Exception e) {
            log.error("Failed to initiate BI-FAST transfer for disbursement: {}", id, e);
            // Don't fail here - let the async callback handle the actual result
        }

        return saveWithOptimisticLockRetry(disbursement);
    }

    private DisbursementEntity saveWithOptimisticLockRetry(DisbursementEntity entity) {
        int maxAttempts = 3;
        org.springframework.orm.ObjectOptimisticLockingFailureException last = null;
        DisbursementEntity current = entity;
        for (int i = 0; i < maxAttempts; i++) {
            final DisbursementEntity toSave = current;
            try {
                return disbursementRepository.save(toSave);
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                last = e;
                log.warn("Optimistic lock attempt {}/{}, re-fetching entity {}", i + 1, maxAttempts, toSave.getId());
                try { Thread.sleep(50L * (i + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                final DisbursementEntity refetched = disbursementRepository.findById(toSave.getId())
                        .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity disappeared: " + toSave.getId()));
                current = refetched;
            }
        }
        throw last;
    }

    @Override
    @Transactional
    public DisbursementEntity completeDisbursement(UUID id, String bankReference) {
        log.info("Completing disbursement: {} with bank reference: {}", id, bankReference);

        // IMP-5: row FOR UPDATE + terminal check — of two concurrent callbacks
        // (COMPLETED racing FAILED) exactly one mutates; the loser is a no-op.
        DisbursementEntity disbursement = disbursementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity not found: " + id));

        if (disbursement.getStatus() != DisbursementStatus.PROCESSING) {
            log.info("Disbursement {} already in terminal status {}, callback no-op", id, disbursement.getStatus());
            return disbursement;
        }

        // Transition to COMPLETED
        disbursement.complete(bankReference);

        walletService.commitBalance(
                disbursement.getSourceAccountId(),
                disbursement.getId().toString(),
                disbursement.getReservationId(),
                disbursement.getAmount().getAmount()
        );

        log.info("DisbursementEntity completed: {}", id);
        return disbursementRepository.save(disbursement);
    }

    @Override
    @Transactional
    public DisbursementEntity failDisbursement(UUID id, String reason) {
        log.info("Failing disbursement: {} with reason: {}", id, reason);

        // IMP-5: row FOR UPDATE + terminal check (see completeDisbursement)
        DisbursementEntity disbursement = disbursementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("DisbursementEntity not found: " + id));

        if (disbursement.getStatus() != DisbursementStatus.PROCESSING) {
            log.info("Disbursement {} already in terminal status {}, callback no-op", id, disbursement.getStatus());
            return disbursement;
        }

        // Transition to FAILED
        disbursement.fail(reason);

        walletService.releaseBalance(
                disbursement.getSourceAccountId(),
                disbursement.getId().toString(),
                disbursement.getReservationId(),
                disbursement.getAmount().getAmount()
        );

        log.info("DisbursementEntity failed: {}", id);
        return disbursementRepository.save(disbursement);
    }

    @Override
    public List<DisbursementEntity> listDisbursementsByStatus(String status, int limit) {
        return disbursementRepository.findByStatus(status, limit);
    }

    private String generateIdempotencyKey() {
        return "disb-" + UUID.randomUUID().toString().replace("-", "");
    }
}
