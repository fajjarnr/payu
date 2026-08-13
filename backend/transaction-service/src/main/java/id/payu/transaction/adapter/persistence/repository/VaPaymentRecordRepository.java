package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.VaPaymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ARCH-TXN-001: insert-only repository for the immutable VA payment ledger.
 * Exposes no update/delete methods on purpose.
 */
@Repository
public interface VaPaymentRecordRepository extends JpaRepository<VaPaymentRecordEntity, UUID> {
}
