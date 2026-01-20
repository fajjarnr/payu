package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.domain.model.LedgerEntry;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.accountId = :accountId ORDER BY le.createdAt DESC")
    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") UUID accountId);

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.transactionId = :transactionId")
    List<LedgerEntryEntity> findByTransactionId(@Param("transactionId") UUID transactionId);
}
