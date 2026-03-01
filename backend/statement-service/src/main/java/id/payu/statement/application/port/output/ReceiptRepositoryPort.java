package id.payu.statement.application.port.output;

import id.payu.statement.domain.model.Receipt;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Receipt persistence.
 * Following Hexagonal Architecture - this is the interface that the domain
 * uses to persist receipts. The actual implementation is in the adapter layer.
 * <p>
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
public interface ReceiptRepositoryPort {

    /**
     * Save a receipt to the database.
     *
     * @param receipt The receipt to save
     * @return The saved receipt with generated ID
     */
    Receipt save(Receipt receipt);

    /**
     * Find a receipt by its ID.
     *
     * @param id The receipt ID
     * @return Optional containing the receipt if found
     */
    Optional<Receipt> findById(UUID id);

    /**
     * Find a receipt by transaction ID.
     *
     * @param transactionId The transaction ID
     * @return Optional containing the receipt if found
     */
    Optional<Receipt> findByTransactionId(String transactionId);

    /**
     * Check if a receipt exists for a transaction.
     *
     * @param transactionId The transaction ID
     * @return true if receipt exists
     */
    boolean existsByTransactionId(String transactionId);
}
