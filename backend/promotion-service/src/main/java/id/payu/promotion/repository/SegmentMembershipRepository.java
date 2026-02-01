package id.payu.promotion.repository;

import id.payu.promotion.domain.SegmentMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SegmentMembershipRepository extends JpaRepository<SegmentMembership, UUID> {

    List<SegmentMembership> findByAccountId(String accountId);

    List<SegmentMembership> findBySegmentId(UUID segmentId);

    boolean existsByAccountIdAndSegmentId(String accountId, UUID segmentId);
}
