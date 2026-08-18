package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.SegmentMembershipEntity;
import id.payu.promotion.domain.model.SegmentMembership;
import org.springframework.stereotype.Component;

@Component
public class SegmentMembershipPersistenceMapper {

    public SegmentMembership toDomain(SegmentMembershipEntity e) {
        return new SegmentMembership(
                e.getId(), e.getAccountId(), e.getSegmentId(),
                e.getIsActive(), e.getLastEvaluatedAt(), e.getCreatedAt(), null);
    }
}
