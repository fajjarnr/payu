package id.payu.promotion.adapter.persistence.repository;

import id.payu.promotion.adapter.persistence.entity.SegmentMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SegmentMembershipRepository extends JpaRepository<SegmentMembershipEntity, UUID> {

    List<SegmentMembershipEntity> findByAccountId(String accountId);

    List<SegmentMembershipEntity> findBySegmentId(UUID segmentId);

    boolean existsByAccountIdAndSegmentId(String accountId, UUID segmentId);
}
