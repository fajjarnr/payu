package id.payu.lending.repository;

import id.payu.lending.entity.RepaymentScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentScheduleEntity, UUID> {
    List<RepaymentScheduleEntity> findByLoanId(UUID loanId);
    Optional<RepaymentScheduleEntity> findByLoanIdAndInstallmentNumber(UUID loanId, Integer installmentNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RepaymentScheduleEntity s where s.id = :id")
    Optional<RepaymentScheduleEntity> findByIdForUpdate(UUID id);
}
