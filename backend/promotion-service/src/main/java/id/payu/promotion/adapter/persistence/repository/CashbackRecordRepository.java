package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.CashbackRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CashbackRecordRepository extends JpaRepository<CashbackRecordEntity, UUID> {
    boolean existsByTransactionId(String transactionId);
}
