package id.payu.promotion.repository;

import id.payu.promotion.domain.Cashback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashbackRepository extends JpaRepository<Cashback, UUID> {

    List<Cashback> findByAccountId(String accountId);

    @Query("SELECT c FROM Cashback c WHERE c.accountId = :accountId AND c.createdAt >= :start AND c.createdAt <= :end")
    List<Cashback> findByAccountIdAndDateRange(@Param("accountId") String accountId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
