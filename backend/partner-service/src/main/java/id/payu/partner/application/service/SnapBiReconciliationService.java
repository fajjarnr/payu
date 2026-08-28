package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.entity.SnapBiPaymentEntity;
import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import id.payu.partner.adapter.persistence.entity.SnapReconciliationCaseEntity;
import id.payu.partner.adapter.persistence.repository.SnapBiPaymentRepository;
import id.payu.partner.adapter.persistence.repository.SnapBiRefundRepository;
import id.payu.partner.adapter.persistence.repository.SnapReconciliationCaseRepository;
import id.payu.partner.domain.port.out.WalletSettlementPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PARTNER-PROD-005: automatic reconciliation of SNAP payments/refunds against
 * the wallet ledger. Every COMPLETED payment must have both ledger legs
 * (DEBIT on source, CREDIT on beneficiary) and every COMPLETED refund a
 * REFUND_REVERSAL movement; any ledger movement without a COMPLETED partner
 * record is an orphan (possible double-debit or crash-after-commit). Each
 * unmatched reference becomes an OPEN reconciliation case — the run ends with
 * zero unmatched or an alert+case for ops (reversal-only correction).
 */
@Service
public class SnapBiReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(SnapBiReconciliationService.class);

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final String REFUND_PREFIX = "REFUND-";

    private final SnapBiPaymentRepository paymentRepository;
    private final SnapBiRefundRepository refundRepository;
    private final SnapReconciliationCaseRepository caseRepository;
    private final WalletSettlementPort walletSettlementPort;

    @Value("${payu.reconciliation.snap.window-hours:24}")
    private long windowHours;

    public SnapBiReconciliationService(SnapBiPaymentRepository paymentRepository,
                                       SnapBiRefundRepository refundRepository,
                                       SnapReconciliationCaseRepository caseRepository,
                                       WalletSettlementPort walletSettlementPort) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.caseRepository = caseRepository;
        this.walletSettlementPort = walletSettlementPort;
    }

    // Visible for testing
    void setWindowHours(long windowHours) {
        this.windowHours = windowHours;
    }

    @SchedulerLock(name = "SnapBiReconciliationService_reconcileSnapMovements",
            lockAtLeastFor = "PT1S", lockAtMostFor = "PT30M")
    @Scheduled(fixedDelayString = "${payu.reconciliation.snap.interval-ms:3600000}")
    @Transactional
    public void reconcileSnapMovements() {
        Instant from = Instant.now().minus(Duration.ofHours(windowHours));

        // All records in the window (any status): COMPLETED rows are matched
        // against their ledger legs; rows still PENDING/FAILED that have ledger
        // movements are orphan cases (crash after wallet commit, before response).
        List<SnapBiPaymentEntity> payments = paymentRepository.findByCreatedAtAfter(from);
        List<SnapBiRefundEntity> refunds = refundRepository.findByCreatedAtAfter(from);

        if (payments.isEmpty() && refunds.isEmpty()) {
            log.info("Snap reconciliation: nothing to reconcile in the last {}h window", windowHours);
            return;
        }

        List<SnapBiPaymentEntity> completedPayments = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .toList();
        List<SnapBiRefundEntity> completedRefunds = refunds.stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()))
                .toList();

        List<String> paymentRefs = payments.stream()
                .map(SnapBiPaymentEntity::getPayuReferenceNo)
                .toList();
        List<String> refundUuids = refunds.stream()
                .map(r -> refundUuid(r.getPayuRefundNo()))
                .toList();

        List<String> referenceIds = new ArrayList<>(paymentRefs);
        referenceIds.addAll(refundUuids);

        Map<String, List<WalletSettlementPort.LedgerMovement>> byRef = walletSettlementPort
                .ledgerMovementsByReferences(referenceIds)
                .stream()
                .collect(Collectors.groupingBy(WalletSettlementPort.LedgerMovement::referenceId));

        int created = 0;

        for (SnapBiPaymentEntity payment : completedPayments) {
            List<WalletSettlementPort.LedgerMovement> moves =
                    byRef.getOrDefault(payment.getPayuReferenceNo(), List.of());
            boolean debitLeg = moves.stream().anyMatch(m ->
                    "DEBIT".equals(m.entryType())
                            && ("RESERVATION".equals(m.referenceType()) || "COMMIT".equals(m.referenceType())));
            boolean creditLeg = moves.stream().anyMatch(m ->
                    "CREDIT".equals(m.entryType()) && "CREDIT".equals(m.referenceType()));
            if (!debitLeg || !creditLeg) {
                created += createCase(SnapReconciliationCaseEntity.TYPE_PAYMENT,
                        payment.getPayuReferenceNo(),
                        "COMPLETED payment missing ledger legs (debit=" + debitLeg + ", credit=" + creditLeg + ")");
            } else {
                // GLOBAL-RECON: auto-resolve crash-after-commit within 5m window
                caseRepository.findByReferenceTypeAndReferenceId(SnapReconciliationCaseEntity.TYPE_PAYMENT, payment.getPayuReferenceNo())
                        .filter(c -> SnapReconciliationCaseEntity.STATUS_OPEN.equals(c.getStatus()))
                        .ifPresent(c -> {
                            if (Duration.between(c.getDetectedAt(), Instant.now()).toMinutes() < 5) {
                                c.resolve();
                                caseRepository.save(c);
                                log.info("Auto-resolved PAYMENT {} within 5m ledger catch-up", payment.getPayuReferenceNo());
                            }
                        });
            }
        }

        for (SnapBiRefundEntity refund : completedRefunds) {
            List<WalletSettlementPort.LedgerMovement> moves =
                    byRef.getOrDefault(refundUuid(refund.getPayuRefundNo()), List.of());
            boolean reversal = moves.stream()
                    .anyMatch(m -> "REFUND_REVERSAL".equals(m.referenceType()));
            if (!reversal) {
                created += createCase(SnapReconciliationCaseEntity.TYPE_REFUND,
                        refund.getPayuRefundNo(),
                        "COMPLETED refund without REFUND_REVERSAL ledger movement");
            } else {
                // GLOBAL-RECON: auto-resolve refund reversal catch-up within 5m
                caseRepository.findByReferenceTypeAndReferenceId(SnapReconciliationCaseEntity.TYPE_REFUND, refund.getPayuRefundNo())
                        .filter(c -> SnapReconciliationCaseEntity.STATUS_OPEN.equals(c.getStatus()))
                        .ifPresent(c -> {
                            if (Duration.between(c.getDetectedAt(), Instant.now()).toMinutes() < 5) {
                                c.resolve();
                                caseRepository.save(c);
                                log.info("Auto-resolved REFUND {} within 5m ledger catch-up", refund.getPayuRefundNo());
                            }
                        });
            }
        }

        Set<String> completedRefs = new HashSet<>();
        completedPayments.forEach(p -> completedRefs.add(p.getPayuReferenceNo()));
        completedRefunds.forEach(r -> completedRefs.add(refundUuid(r.getPayuRefundNo())));

        for (Map.Entry<String, List<WalletSettlementPort.LedgerMovement>> entry : byRef.entrySet()) {
            String ref = entry.getKey();
            if (isSnapReference(ref) && !completedRefs.contains(ref)) {
                created += createCase(SnapReconciliationCaseEntity.TYPE_WALLET_MOVEMENT,
                        ref,
                        "wallet ledger movement without a COMPLETED partner record ("
                                + entry.getValue().size() + " movement(s))");
            }
        }
        // GLOBAL-RECON: auto-resolve wallet movement orphans when partner record appears within 5m (reverse crash)
        for (String ref : completedRefs) {
            caseRepository.findByReferenceTypeAndReferenceId(SnapReconciliationCaseEntity.TYPE_WALLET_MOVEMENT, ref)
                    .filter(c -> SnapReconciliationCaseEntity.STATUS_OPEN.equals(c.getStatus()))
                    .ifPresent(c -> {
                        if (Duration.between(c.getDetectedAt(), Instant.now()).toMinutes() < 5) {
                            c.resolve();
                            caseRepository.save(c);
                            log.info("Auto-resolved WALLET_MOVEMENT {} within 5m partner catch-up", ref);
                        }
                    });
        }


        if (created == 0) {
            log.info("Snap reconciliation clean: {} payment(s), {} refund(s), {} reference(s) checked, 0 unmatched",
                    payments.size(), refunds.size(), byRef.size());
        } else {
            log.warn("Snap reconciliation found {} unmatched case(s) over {} payment(s), {} refund(s) — "
                            + "check snap_reconciliation_cases",
                    created, payments.size(), refunds.size());
        }
    }

    private int createCase(String referenceType, String referenceId, String detail) {
        if (caseRepository.existsByReferenceTypeAndReferenceId(referenceType, referenceId)) {
            log.info("Reconciliation case already open for {} {}", referenceType, referenceId);
            return 0;
        }
        caseRepository.save(new SnapReconciliationCaseEntity(referenceType, referenceId, detail));
        log.warn("Reconciliation case OPEN: type={} reference={} detail={}", referenceType, referenceId, detail);
        return 1;
    }

    private static String refundUuid(String payuRefundNo) {
        if (payuRefundNo != null && payuRefundNo.startsWith(REFUND_PREFIX)) {
            return payuRefundNo.substring(REFUND_PREFIX.length());
        }
        return payuRefundNo;
    }

    private static boolean isSnapReference(String ref) {
        if (ref == null) {
            return false;
        }
        return ref.startsWith("PAYU-") || (ref.length() == 36 && UUID_PATTERN.matcher(ref).matches());
    }
}
