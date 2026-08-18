package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.SegmentMembershipEntity;
import id.payu.promotion.adapter.persistence.repository.SegmentMembershipRepository;
import id.payu.promotion.domain.model.SegmentMembership;
import id.payu.promotion.domain.port.out.SegmentMembershipPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SegmentMembershipPersistenceAdapter implements SegmentMembershipPersistencePort {

    private final SegmentMembershipRepository repository;
    private final SegmentMembershipPersistenceMapper mapper;

    public SegmentMembershipPersistenceAdapter(SegmentMembershipRepository repository,
                                               SegmentMembershipPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<SegmentMembership> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SegmentMembership> findBySegmentId(UUID segmentId) {
        return repository.findBySegmentId(segmentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countBySegmentId(UUID segmentId) {
        return repository.findBySegmentId(segmentId).size();
    }
}
