package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.RefundReversalExecutionEntity;
import id.payu.wallet.domain.model.RefundReversalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundReversalExecutionRepository extends JpaRepository<RefundReversalExecutionEntity, UUID> {
    Optional<RefundReversalExecutionEntity> findByRefundId(UUID refundId);
    List<RefundReversalExecutionEntity> findByStatusIn(Collection<RefundReversalStatus> statuses);
}
