package id.payu.fx.adapter.persistence.repository;

import id.payu.fx.adapter.persistence.entity.FxRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FxRateJpaRepository extends JpaRepository<FxRateEntity, UUID> {

    @Query("SELECT r FROM FxRateEntity r WHERE r.fromCurrency = :fromCurrency AND r.toCurrency = :toCurrency " +
           "AND r.validFrom <= :timestamp AND r.validUntil > :timestamp ORDER BY r.validFrom DESC")
    Optional<FxRateEntity> findLatestValidRate(@Param("fromCurrency") String fromCurrency,
                                                @Param("toCurrency") String toCurrency,
                                                @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT r FROM FxRateEntity r WHERE r.fromCurrency = :fromCurrency AND r.toCurrency = :toCurrency " +
           "ORDER BY r.validFrom DESC")
    List<FxRateEntity> findByCurrencyPair(@Param("fromCurrency") String fromCurrency,
                                          @Param("toCurrency") String toCurrency);

    @Query("DELETE FROM FxRateEntity r WHERE r.validUntil < :before")
    void deleteExpiredRates(@Param("before") LocalDateTime before);
}
