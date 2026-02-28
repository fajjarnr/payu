package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.model.DisbursementStatus;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.DisbursementUseCase;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * @see Disbursement
 * @see DisbursementUseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisbursementService implements DisbursementUseCase {

    private final DisbursementRepositoryPort disbursementRepository;
    private final WalletServicePort walletService;
    private final BifastServicePort bifastService;

    @Override
    @Transactional
    public Disbursement createDisbursement(
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
            Optional<Disbursement> existing = disbursementRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning existing disbursement for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // Create disbursement
        Disbursement disbursement = Disbursement.createWithIdempotencyKey(
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

        // Save disbursement
        Disbursement saved = disbursementRepository.save(disbursement);
        log.info("Created disbursement: {}", saved.getId());

        return saved;
    }

    @Override
    public Optional<Disbursement> getDisbursement(UUID id) {
        return disbursementRepository.findById(id);
    }

    @Override
    public Optional<Disbursement> findByIdempotencyKey(String idempotencyKey) {
        return disbursementRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Disbursement> listDisbursementsByAccount(UUID sourceAccountId, int limit, int offset) {
        return disbursementRepository.findBySourceAccountId(sourceAccountId, limit, offset);
    }

    @Override
    @Transactional
    public Disbursement processDisbursement(UUID id) {
        log.info("Processing disbursement: {}", id);

        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + id));

        // Transition to PROCESSING
        disbursement.process();

        // Initiate BI-FAST transfer
        BifastTransferRequest request = BifastTransferRequest.builder()
                .referenceNumber(disbursement.getId().toString())
                .beneficiaryBankCode(disbursement.getBankCode())
                .beneficiaryAccountNumber(disbursement.getAccountNumber())
                .beneficiaryAccountName(disbursement.getAccountName())
                .amount(disbursement.getAmount().getAmount())
                .currency(disbursement.getAmount().getCurrency().getCurrencyCode())
                .senderAccountNumber("PAYU" + disbursement.getSourceAccountId().toString().substring(0, 8))
                .senderAccountName("PayU Disbursement")
                .purposeCode("PAY")
                .build();

        try {
            bifastService.initiateTransfer(request);
            log.info("BI-FAST transfer initiated for disbursement: {}", id);
        } catch (Exception e) {
            log.error("Failed to initiate BI-FAST transfer for disbursement: {}", id, e);
            // Don't fail here - let the async callback handle the actual result
        }

        return disbursementRepository.save(disbursement);
    }

    @Override
    @Transactional
    public Disbursement completeDisbursement(UUID id, String bankReference) {
        log.info("Completing disbursement: {} with bank reference: {}", id, bankReference);

        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + id));

        // Transition to COMPLETED
        disbursement.complete(bankReference);

        // Commit the reserved balance
        walletService.commitBalance(
                disbursement.getSourceAccountId(),
                disbursement.getId().toString(),
                null, // reservationId would be stored in a real implementation
                disbursement.getAmount().getAmount()
        );

        log.info("Disbursement completed: {}", id);
        return disbursementRepository.save(disbursement);
    }

    @Override
    @Transactional
    public Disbursement failDisbursement(UUID id, String reason) {
        log.info("Failing disbursement: {} with reason: {}", id, reason);

        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + id));

        // Transition to FAILED
        disbursement.fail(reason);

        // Release the reserved balance
        walletService.releaseBalance(
                disbursement.getSourceAccountId(),
                disbursement.getId().toString(),
                null, // reservationId would be stored in a real implementation
                disbursement.getAmount().getAmount()
        );

        log.info("Disbursement failed: {}", id);
        return disbursementRepository.save(disbursement);
    }

    @Override
    public List<Disbursement> listDisbursementsByStatus(String status, int limit) {
        return disbursementRepository.findByStatus(status, limit);
    }

    private String generateIdempotencyKey() {
        return "disb-" + UUID.randomUUID().toString().replace("-", "");
    }
}
