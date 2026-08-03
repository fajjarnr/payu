package id.payu.loanorigination.adapter.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoanOriginationProcessRepository extends JpaRepository<LoanOriginationProcessEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select process from LoanOriginationProcessEntity process where process.id = :id")
    Optional<LoanOriginationProcessEntity> findByIdForUpdate(@Param("id") UUID id);
}
