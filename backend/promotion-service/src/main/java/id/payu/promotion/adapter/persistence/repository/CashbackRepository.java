package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.CashbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashbackRepository extends JpaRepository<CashbackEntity, UUID> {

    List<CashbackEntity> findByAccountId(String accountId);

    @Query("SELECT c FROM CashbackEntity c WHERE c.accountId = :accountId AND c.createdAt >= :start AND c.createdAt <= :end")
    List<CashbackEntity> findByAccountIdAndDateRange(@Param("accountId") String accountId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
