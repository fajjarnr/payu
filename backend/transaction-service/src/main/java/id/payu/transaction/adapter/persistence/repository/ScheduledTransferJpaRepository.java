package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.transaction.domain.model.ScheduledStatus;

@Repository
public interface ScheduledTransferJpaRepository extends JpaRepository<ScheduledTransferEntity, UUID> {

    Optional<ScheduledTransferEntity> findByReferenceNumber(String referenceNumber);

    List<ScheduledTransferEntity> findBySenderAccountId(UUID senderAccountId);

    @Query("SELECT st FROM ScheduledTransferEntity st WHERE st.status = 'ACTIVE' AND st.nextExecutionDate <= :now")
    List<ScheduledTransferEntity> findDueScheduledTransfers(@Param("now") Instant now);

    @Query("SELECT st FROM ScheduledTransferEntity st WHERE st.senderAccountId = :accountId AND st.status IN :statuses")
    List<ScheduledTransferEntity> findBySenderAccountIdAndStatusIn(
            @Param("accountId") UUID accountId,
            @Param("statuses") List<ScheduledStatus> statuses
    );
}
