package id.payu.lending.repository;

import id.payu.lending.entity.LoanPreApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanPreApprovalRepository extends JpaRepository<LoanPreApprovalEntity, UUID> {

    Optional<LoanPreApprovalEntity> findByUserId(UUID userId);

    @Query("SELECT lpa FROM LoanPreApprovalEntity lpa WHERE lpa.userId = :userId " +
           "AND lpa.validUntil >= :currentDate AND lpa.status IN ('APPROVED', 'CONDITIONALLY_APPROVED') " +
           "ORDER BY lpa.createdAt DESC")
    Optional<LoanPreApprovalEntity> findActiveByUserId(UUID userId, LocalDate currentDate);
}
