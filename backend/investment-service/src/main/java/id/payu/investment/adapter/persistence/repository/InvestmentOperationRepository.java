package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.InvestmentOperationEntity;
import id.payu.investment.domain.model.InvestmentOperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentOperationRepository extends JpaRepository<InvestmentOperationEntity, UUID> {

    Optional<InvestmentOperationEntity> findByIdempotencyKey(String idempotencyKey);

    List<InvestmentOperationEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Collection<InvestmentOperationStatus> statuses, LocalDateTime now);
}
