package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.SegmentMembership;

import java.util.List;
import java.util.UUID;

/**
 * BE-PROMO-001: persistence port for segment memberships.
 */
public interface SegmentMembershipPersistencePort {

    List<SegmentMembership> findByAccountId(String accountId);

    List<SegmentMembership> findBySegmentId(UUID segmentId);

    long countBySegmentId(UUID segmentId);
}
