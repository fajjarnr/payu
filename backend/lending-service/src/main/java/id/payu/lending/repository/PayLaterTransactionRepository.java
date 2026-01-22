package id.payu.lending.repository;

import id.payu.lending.entity.PayLaterTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayLaterTransactionRepository extends JpaRepository<PayLaterTransactionEntity, UUID> {
    List<PayLaterTransactionEntity> findByPaylaterAccountId(UUID paylaterAccountId);
    List<PayLaterTransactionEntity> findByPaylaterAccountIdOrderByTransactionDateDesc(UUID paylaterAccountId);
    Optional<PayLaterTransactionEntity> findByExternalId(String externalId);
    List<PayLaterTransactionEntity> findByPaylaterAccountIdAndTransactionDateBetween(
            UUID paylaterAccountId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
