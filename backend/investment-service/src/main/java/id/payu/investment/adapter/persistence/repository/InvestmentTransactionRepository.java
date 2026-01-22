package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.InvestmentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransactionEntity, UUID> {
    Optional<InvestmentTransactionEntity> findByAccountId(String accountId);
    List<InvestmentTransactionEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);
    Optional<InvestmentTransactionEntity> findByReferenceNumber(String referenceNumber);
}
