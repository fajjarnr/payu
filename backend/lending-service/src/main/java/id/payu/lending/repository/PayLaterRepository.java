package id.payu.lending.repository;

import id.payu.lending.entity.PayLaterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayLaterRepository extends JpaRepository<PayLaterEntity, UUID> {
    Optional<PayLaterEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayLaterEntity p where p.userId = :userId")
    Optional<PayLaterEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
