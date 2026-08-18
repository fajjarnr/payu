package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.CustomerSegmentEntity;
import id.payu.promotion.domain.model.CustomerSegment;
import org.springframework.stereotype.Component;

@Component
public class CustomerSegmentPersistenceMapper {

    public CustomerSegment toDomain(CustomerSegmentEntity e) {
        return new CustomerSegment(
                e.getId(), e.getName(), e.getDescription(), e.getRules(),
                e.getIsActive(), e.getPriority(), e.getCreatedAt(), e.getUpdatedAt(), null);
    }

    public CustomerSegmentEntity toEntity(CustomerSegment s) {
        CustomerSegmentEntity e = new CustomerSegmentEntity();
        e.setId(s.id());
        e.setName(s.name());
        e.setDescription(s.description());
        e.setRules(s.rules());
        e.setIsActive(s.isActive() != null ? s.isActive() : true);
        e.setPriority(s.priority() != null ? s.priority() : 0);
        e.setCreatedAt(s.createdAt());
        e.setUpdatedAt(s.updatedAt());
        return e;
    }
}
