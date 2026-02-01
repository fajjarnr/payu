package id.payu.promotion.repository;

import id.payu.promotion.domain.DailyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyCheckinRepository extends JpaRepository<DailyCheckin, UUID> {

    Optional<DailyCheckin> findByAccountIdAndCheckinDate(String accountId, LocalDate checkinDate);

    @Query("SELECT COALESCE(SUM(c.pointsEarned), 0) FROM DailyCheckin c WHERE c.accountId = :accountId AND c.checkinDate >= :startDate")
    int sumPointsEarnedSince(@Param("accountId") String accountId, @Param("startDate") LocalDate startDate);

    List<DailyCheckin> findByAccountIdOrderByCheckinDateDesc(String accountId);
}
