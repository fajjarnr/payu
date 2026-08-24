package id.payu.transaction.adapter.web;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/snap/v1.0/transfer")
public class TransferStatusController {
    private final TransactionPersistencePort transactionPersistencePort;

    public TransferStatusController(TransactionPersistencePort transactionPersistencePort) {
        this.transactionPersistencePort = transactionPersistencePort;
    }

    /**
     * SNAP-BI transfer/status inquiry — BRIAPI latestTransactionStatus 00/01/03/06.
     * PADG 14/2025 reconciliation polls this endpoint for PENDING >5m.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTransferStatus(@RequestParam String referenceNo) {
        var txOpt = transactionPersistencePort.findByReferenceNumber(referenceNo);
        if (txOpt.isEmpty()) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("referenceNo", referenceNo);
            notFound.put("latestTransactionStatus", "03");
            notFound.put("transactionStatus", "NOT_FOUND");
            return ResponseEntity.status(404).body(notFound);
        }
        TransactionEntity tx = txOpt.get(0);
        String snapStatus = toSnapStatus(tx.getStatus());
        Map<String, Object> body = new HashMap<>();
        body.put("referenceNo", tx.getReferenceNumber());
        body.put("transactionId", tx.getId().toString());
        body.put("latestTransactionStatus", snapStatus);
        body.put("transactionStatus", tx.getStatus().name());
        body.put("amount", tx.getAmount() != null ? tx.getAmount().getAmount() : tx.getAmountValue());
        body.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        return ResponseEntity.ok(body);
    }

    private String toSnapStatus(TransactionStatus status) {
        return switch (status) {
            case COMPLETED -> "00";
            case PENDING, PROCESSING, VALIDATING, PENDING_COMPLIANCE_REVIEW, PENDING_STEP_UP -> "01";
            case FAILED, CANCELLED -> "03";
        };
    }
}
