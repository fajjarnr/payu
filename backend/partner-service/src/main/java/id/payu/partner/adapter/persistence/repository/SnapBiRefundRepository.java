package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.SnapBiRefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * BUG-BE-182 FIX: JPA repository for SNAP BI refund persistence.
 */
@Repository
public interface SnapBiRefundRepository extends JpaRepository<SnapBiRefundEntity, Long> {

    Optional<SnapBiRefundEntity> findByPayuRefundNo(String payuRefundNo);

    List<SnapBiRefundEntity> findByPayuReferenceNo(String payuReferenceNo);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM SnapBiRefundEntity r WHERE r.payuReferenceNo = :payuReferenceNo")
    BigDecimal sumRefundedAmountByPayuReferenceNo(@Param("payuReferenceNo") String payuReferenceNo);
}
