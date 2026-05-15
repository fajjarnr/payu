package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.SettlementBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import id.payu.wallet.adapter.persistence.entity.SettlementStatus;

@Repository
public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatchEntity, UUID> {

    List<SettlementBatchEntity> findByPartnerIdAndSettlementDateBetween(
            String partnerId, LocalDate from, LocalDate to);

    List<SettlementBatchEntity> findBySettlementDate(LocalDate settlementDate);

    List<SettlementBatchEntity> findByStatus(SettlementStatus status);

    @Query("SELECT s FROM SettlementBatchEntity s WHERE s.partnerId = :partnerId AND s.settlementDate = :date")
    List<SettlementBatchEntity> findByPartnerIdAndSettlementDate(
            @Param("partnerId") String partnerId, @Param("date") LocalDate date);

    @Query("SELECT s FROM SettlementBatchEntity s WHERE s.status = 'PENDING' OR s.status = 'PROCESSING'")
    List<SettlementBatchEntity> findPendingBatches();
}
