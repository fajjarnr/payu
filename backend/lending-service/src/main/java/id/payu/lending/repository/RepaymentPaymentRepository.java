package id.payu.lending.repository;

import id.payu.lending.domain.model.RepaymentPaymentStatus;
import id.payu.lending.entity.RepaymentPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepaymentPaymentRepository extends JpaRepository<RepaymentPaymentEntity, UUID> {
    Optional<RepaymentPaymentEntity> findByIdempotencyKey(String idempotencyKey);
    List<RepaymentPaymentEntity> findByStatusIn(List<RepaymentPaymentStatus> statuses);
}
