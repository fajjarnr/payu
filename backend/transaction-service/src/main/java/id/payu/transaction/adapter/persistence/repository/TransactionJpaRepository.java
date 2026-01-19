package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {
    
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.recipientAccountId = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") UUID accountId, org.springframework.data.domain.Pageable pageable);
}
