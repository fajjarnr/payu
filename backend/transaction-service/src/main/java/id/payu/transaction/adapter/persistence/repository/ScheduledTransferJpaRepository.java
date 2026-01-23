package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledTransferJpaRepository extends JpaRepository<ScheduledTransfer, UUID> {

    Optional<ScheduledTransfer> findByReferenceNumber(String referenceNumber);

    List<ScheduledTransfer> findBySenderAccountId(UUID senderAccountId);

    @Query("SELECT st FROM ScheduledTransfer st WHERE st.status = 'ACTIVE' AND st.nextExecutionDate <= :now")
    List<ScheduledTransfer> findDueScheduledTransfers(@Param("now") Instant now);

    @Query("SELECT st FROM ScheduledTransfer st WHERE st.senderAccountId = :accountId AND st.status IN :statuses")
    List<ScheduledTransfer> findBySenderAccountIdAndStatusIn(
            @Param("accountId") UUID accountId,
            @Param("statuses") List<ScheduledTransfer.ScheduledStatus> statuses
    );
}
