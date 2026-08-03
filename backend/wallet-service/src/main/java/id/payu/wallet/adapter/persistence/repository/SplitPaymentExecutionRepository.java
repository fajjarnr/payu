package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.SplitPaymentExecutionEntity;
import id.payu.wallet.adapter.persistence.entity.SplitExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitPaymentExecutionRepository extends JpaRepository<SplitPaymentExecutionEntity, UUID> {

    Optional<SplitPaymentExecutionEntity> findByIdempotencyKey(String idempotencyKey);

    List<SplitPaymentExecutionEntity> findByPayerAccountIdOrderByCreatedAtDesc(String payerAccountId);

    List<SplitPaymentExecutionEntity> findByStatusIn(List<SplitExecutionStatus> statuses);

    List<SplitPaymentExecutionEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
}
