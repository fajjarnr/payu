package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.CashbackRecordEntity;
import id.payu.promotion.domain.model.CashbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CashbackRecordRepository extends JpaRepository<CashbackRecordEntity, UUID> {
    boolean existsByTransactionIdAndStatus(String transactionId, CashbackStatus status);
    Optional<CashbackRecordEntity> findByTransactionIdAndRuleId(String transactionId, String ruleId);
}
