package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.wallet.adapter.persistence.entity.JournalStatus;

@Repository
public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    Optional<JournalEntryEntity> findByJournalNumber(String journalNumber);

    @EntityGraph(attributePaths = "entries")
    @Query("SELECT j FROM JournalEntryEntity j WHERE j.referenceType = :refType AND j.referenceId = :refId")
    List<JournalEntryEntity> findByReference(@Param("refType") String referenceType,
                                             @Param("refId") String referenceId);

    @Query("SELECT j FROM JournalEntryEntity j WHERE j.status = :status ORDER BY j.createdAt DESC")
    List<JournalEntryEntity> findByStatus(@Param("status") JournalStatus status);

    @Query("SELECT j FROM JournalEntryEntity j WHERE j.postedAt BETWEEN :from AND :to ORDER BY j.postedAt")
    List<JournalEntryEntity> findByPostedAtBetween(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
