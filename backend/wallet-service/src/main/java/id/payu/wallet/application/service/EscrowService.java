package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.EscrowTransaction;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.in.EscrowUseCase;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.in.WalletUseCase;
import id.payu.wallet.domain.port.out.EscrowPersistencePort;
import id.payu.wallet.domain.port.out.WalletEventPublisherPort;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.EscrowStatus;

/**
 * Application service for escrow / payment holding.
 * <p>
 * Orchestrates wallet reservations, double-entry journals, and escrow lifecycle.
 * <p>
 * CoA codes used:
 * <ul>
 *   <li>1100 — User Wallets (ASSET)</li>
 *   <li>2100 — Escrow Holdings (LIABILITY)</li>
 *   <li>2200 — Merchant Payable (LIABILITY)</li>
 * </ul>
 */
@Service
public class EscrowService implements EscrowUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EscrowService.class);

    private static final String COA_USER_WALLETS = "1100";
    private static final String COA_ESCROW_HOLDINGS = "2100";
    private static final String COA_MERCHANT_PAYABLE = "2200";
    private static final String REFERENCE_TYPE = "ESCROW";
    private static final int DEFAULT_EXPIRY_HOURS = 72;

    private final EscrowPersistencePort escrowPersistencePort;
    private final WalletUseCase walletUseCase;
    private final JournalUseCase journalUseCase;
    private final WalletEventPublisherPort eventPublisher;

    public EscrowService(EscrowPersistencePort escrowPersistencePort,
                         WalletUseCase walletUseCase,
                         JournalUseCase journalUseCase,
                         WalletEventPublisherPort eventPublisher) {
        this.escrowPersistencePort = escrowPersistencePort;
        this.walletUseCase = walletUseCase;
        this.journalUseCase = journalUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public EscrowTransaction createAndHoldEscrow(String buyerAccountId, String sellerAccountId,
                                                  String partnerId, BigDecimal amount,
                                                  BigDecimal feeAmount, String currency,
                                                  String externalReferenceId, String description,
                                                  int expiresInHours) {
        log.info("Creating escrow: buyer={}, seller={}, partner={}, amount={} {}",
                maskId(buyerAccountId), maskId(sellerAccountId), partnerId, amount, currency);

        if (expiresInHours <= 0) {
            expiresInHours = DEFAULT_EXPIRY_HOURS;
        }

        // 1. Create escrow domain object
        EscrowTransaction escrow = EscrowTransaction.builder()
                .id(UUID.randomUUID())
                .buyerAccountId(buyerAccountId)
                .sellerAccountId(sellerAccountId)
                .partnerId(partnerId)
                .amount(amount)
                .feeAmount(feeAmount != null ? feeAmount : BigDecimal.ZERO)
                .currency(currency != null ? currency : "IDR")
                .status(EscrowStatus.CREATED)
                .externalReferenceId(externalReferenceId)
                .description(description)
                .expiresAt(LocalDateTime.now().plusHours(expiresInHours))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 2. Reserve buyer funds (throws InsufficientBalanceException if insufficient)
        String reservationId = walletUseCase.reserveBalance(
                buyerAccountId, amount, escrow.getId().toString());

        // 3. Transition to HELD
        escrow.hold(reservationId);

        // 4. Create double-entry journal: DR Buyer Wallet (1100) / CR Escrow Holdings (2100)
        createHoldJournal(escrow);

        // 5. Persist escrow
        EscrowTransaction saved = escrowPersistencePort.save(escrow);

        // 6. Publish escrow held event (within same transaction via outbox)
        eventPublisher.publishEscrowHeld(saved.getId(), saved.getBuyerAccountId(),
                saved.getSellerAccountId(), saved.getPartnerId(), saved.getAmount(),
                saved.getCurrency(), saved.getExternalReferenceId());

        log.info("Escrow created and held: id={}, reservationId={}", saved.getId(), reservationId);
        return saved;
    }

    @Override
    @Transactional
    public EscrowTransaction releaseEscrow(UUID escrowId) {
        log.info("Releasing escrow: id={}", escrowId);

        EscrowTransaction escrow = findEscrowOrThrow(escrowId);
        escrow.release();

        // Journal: DR Escrow Holdings (2100) / CR Merchant Payable (2200)
        createReleaseJournal(escrow);

        EscrowTransaction saved = escrowPersistencePort.save(escrow);

        // Publish escrow released event
        eventPublisher.publishEscrowReleased(saved.getId(), saved.getPartnerId(),
                saved.getAmount(), saved.getCurrency());

        log.info("Escrow released: id={}", escrowId);
        return saved;
    }

    @Override
    @Transactional
    public EscrowTransaction settleEscrow(UUID escrowId) {
        log.info("Settling escrow: id={}", escrowId);

        EscrowTransaction escrow = findEscrowOrThrow(escrowId);
        escrow.settle();

        // Credit merchant wallet with net amount (amount - fee)
        BigDecimal netAmount = escrow.getNetAmount();
        walletUseCase.credit(
                escrow.getSellerAccountId(),
                netAmount,
                escrow.getId().toString(),
                "Escrow settlement: " + escrow.getDescription());

        // Journal: DR Merchant Payable (2200) / CR Merchant Wallet (1100)
        createSettlementJournal(escrow, netAmount);

        EscrowTransaction saved = escrowPersistencePort.save(escrow);

        // Publish escrow settled event
        eventPublisher.publishEscrowSettled(saved.getId(), saved.getSellerAccountId(),
                saved.getPartnerId(), netAmount, saved.getCurrency());

        log.info("Escrow settled: id={}, netAmount={}", escrowId, netAmount);
        return saved;
    }

    @Override
    @Transactional
    public EscrowTransaction refundEscrow(UUID escrowId, String reason) {
        log.info("Refunding escrow: id={}, reason={}", escrowId, reason);

        EscrowTransaction escrow = findEscrowOrThrow(escrowId);

        // If still HELD, release the reservation back first (this restores the buyer's available balance)
        boolean wasHeld = escrow.getStatus() == EscrowStatus.HELD;
        if (wasHeld) {
            walletUseCase.releaseReservation(escrow.getReservationId());
        }

        escrow.refund(reason);

        // Only credit buyer wallet if NOT held (e.g., EXPIRED) — for HELD status,
        // releaseReservation already restored the balance; crediting again would double-credit.
        if (!wasHeld) {
            walletUseCase.credit(
                    escrow.getBuyerAccountId(),
                    escrow.getAmount(),
                    escrow.getId().toString(),
                    "Escrow refund: " + reason);
        }

        // Journal: DR Escrow Holdings (2100) / CR Buyer Wallet (1100)
        createRefundJournal(escrow);

        EscrowTransaction saved = escrowPersistencePort.save(escrow);

        // Publish escrow refunded event
        eventPublisher.publishEscrowRefunded(saved.getId(), saved.getBuyerAccountId(),
                saved.getPartnerId(), saved.getAmount(), saved.getCurrency(), reason);

        log.info("Escrow refunded: id={}", escrowId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public EscrowTransaction getEscrow(UUID escrowId) {
        return findEscrowOrThrow(escrowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscrowTransaction> getEscrowsByBuyer(String buyerAccountId) {
        return escrowPersistencePort.findByBuyerAccountId(buyerAccountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscrowTransaction> getEscrowsBySeller(String sellerAccountId) {
        return escrowPersistencePort.findBySellerAccountId(sellerAccountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscrowTransaction> getEscrowsByPartner(String partnerId) {
        return escrowPersistencePort.findByPartnerId(partnerId);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${escrow.expiry-check-interval-ms:300000}")
    public void processExpiredEscrows() {
        List<EscrowTransaction> expired = escrowPersistencePort.findExpiredHeldEscrows(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }

        log.info("Processing {} expired escrows", expired.size());
        for (EscrowTransaction escrow : expired) {
            try {
                escrow.expire();
                escrowPersistencePort.save(escrow);

                // Publish escrow expired event before auto-refund
                eventPublisher.publishEscrowExpired(escrow.getId(), escrow.getPartnerId(),
                        escrow.getAmount(), escrow.getCurrency());

                refundEscrow(escrow.getId(), "Auto-refund: escrow expired");
            } catch (Exception e) {
                log.error("Failed to process expired escrow: id={}", escrow.getId(), e);
            }
        }
    }

    // --- Private helpers ---

    private EscrowTransaction findEscrowOrThrow(UUID escrowId) {
        return escrowPersistencePort.findById(escrowId)
                .orElseThrow(() -> new EscrowNotFoundException(escrowId.toString()));
    }

    private void createHoldJournal(EscrowTransaction escrow) {
        List<LedgerEntry> entries = Arrays.asList(
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(escrow.getBuyerAccountId())
                        .coaCode(COA_USER_WALLETS)
                        .entryType(EntryType.DEBIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build(),
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId("SYSTEM_ESCROW")
                        .coaCode(COA_ESCROW_HOLDINGS)
                        .entryType(EntryType.CREDIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        journalUseCase.createAndPostJournal(
                "Escrow hold: " + escrow.getDescription(),
                REFERENCE_TYPE,
                escrow.getId().toString(),
                entries,
                "escrow-service");
    }

    private void createReleaseJournal(EscrowTransaction escrow) {
        List<LedgerEntry> entries = Arrays.asList(
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId("SYSTEM_ESCROW")
                        .coaCode(COA_ESCROW_HOLDINGS)
                        .entryType(EntryType.DEBIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build(),
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(escrow.getSellerAccountId())
                        .coaCode(COA_MERCHANT_PAYABLE)
                        .entryType(EntryType.CREDIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        journalUseCase.createAndPostJournal(
                "Escrow release: " + escrow.getDescription(),
                REFERENCE_TYPE,
                escrow.getId().toString(),
                entries,
                "escrow-service");
    }

    private void createSettlementJournal(EscrowTransaction escrow, BigDecimal netAmount) {
        List<LedgerEntry> entries = Arrays.asList(
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(escrow.getSellerAccountId())
                        .coaCode(COA_MERCHANT_PAYABLE)
                        .entryType(EntryType.DEBIT)
                        .amount(netAmount)
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build(),
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(escrow.getSellerAccountId())
                        .coaCode(COA_USER_WALLETS)
                        .entryType(EntryType.CREDIT)
                        .amount(netAmount)
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        journalUseCase.createAndPostJournal(
                "Escrow settlement: " + escrow.getDescription(),
                REFERENCE_TYPE,
                escrow.getId().toString(),
                entries,
                "escrow-service");
    }

    private void createRefundJournal(EscrowTransaction escrow) {
        List<LedgerEntry> entries = Arrays.asList(
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId("SYSTEM_ESCROW")
                        .coaCode(COA_ESCROW_HOLDINGS)
                        .entryType(EntryType.DEBIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build(),
                LedgerEntry.builder()
                        .id(UUID.randomUUID())
                        .accountId(escrow.getBuyerAccountId())
                        .coaCode(COA_USER_WALLETS)
                        .entryType(EntryType.CREDIT)
                        .amount(escrow.getAmount())
                        .currency(escrow.getCurrency())
                        .referenceType(REFERENCE_TYPE)
                        .referenceId(escrow.getId().toString())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        journalUseCase.createAndPostJournal(
                "Escrow refund: " + escrow.getRefundReason(),
                REFERENCE_TYPE,
                escrow.getId().toString(),
                entries,
                "escrow-service");
    }

    private String maskId(String id) {
        if (id == null || id.length() <= 4) return "****";
        return id.substring(0, 4) + "****";
    }
}
