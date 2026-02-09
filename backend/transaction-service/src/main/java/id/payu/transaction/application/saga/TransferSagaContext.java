package id.payu.transaction.application.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga context for inter-bank transfer orchestration (BiFast/SKN/RGS).
 * <p>
 * Carries all the state needed across saga steps:
 * - Transaction identity
 * - Sender/recipient details
 * - Reservation tracking for compensation
 * <p>
 * This context is persisted as JSONB in the saga_instances table,
 * enabling recovery after crashes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSagaContext {

    /** The transaction ID being processed */
    private UUID transactionId;

    /** Reference number for idempotency */
    private String referenceNumber;

    /** Sender's account ID for wallet operations */
    private UUID senderAccountId;

    /** Recipient's account number (external bank) */
    private String recipientAccountNumber;

    /** Recipient's bank code */
    private String recipientBankCode;

    /** Transfer amount in IDR */
    private BigDecimal amount;

    /** Currency code */
    private String currency;

    /** Transfer channel: BIFAST, SKN, RGS */
    private String transferType;

    // --- Intermediate state for compensation ---

    /** Wallet reservation ID (set after RESERVE_BALANCE step) */
    private String reservationId;

    /** External transfer reference from BiFast/SKN/RGS (set after INITIATE_TRANSFER step) */
    private String externalTransferReference;

    /** Flag indicating if balance was reserved (for compensation) */
    private boolean balanceReserved;

    /** Flag indicating if external transfer was initiated */
    private boolean externalTransferInitiated;
}
