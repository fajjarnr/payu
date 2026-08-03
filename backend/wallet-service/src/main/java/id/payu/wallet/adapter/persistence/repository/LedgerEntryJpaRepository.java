package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.accountId = :accountId ORDER BY le.createdAt DESC")
    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") String accountId);

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.transactionId = :transactionId")
    List<LedgerEntryEntity> findByTransactionId(@Param("transactionId") UUID transactionId);

    Optional<LedgerEntryEntity> findFirstByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.coaCode = :coaCode ORDER BY le.createdAt")
    List<LedgerEntryEntity> findByCoaCode(@Param("coaCode") String coaCode);

    @Query("SELECT le FROM LedgerEntryEntity le WHERE le.coaCode = :coaCode AND le.createdAt BETWEEN :from AND :to ORDER BY le.createdAt")
    List<LedgerEntryEntity> findByCoaCodeAndCreatedAtBetween(
            @Param("coaCode") String coaCode,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
