package id.payu.backoffice.adapter.persistence;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.domain.CustomerCase;
import org.springframework.stereotype.Component;

@Component
public class CustomerCaseMapper {
    public CustomerCase toDomain(CustomerCaseEntity entity) {
        return CustomerCase.reconstitute(entity.getId(), entity.getUserId(), entity.getAccountNumber(),
                entity.getCaseNumber(), entity.getCaseType(), entity.getPriority(), entity.getSubject(),
                entity.getDescription(), entity.getStatus(), entity.getNotes(), entity.getAssignedTo(),
                entity.getResolvedBy(), entity.getResolvedAt(), entity.getCreatedAt(), entity.getVersion());
    }

    public CustomerCaseEntity toEntity(CustomerCase domain) {
        return CustomerCaseEntity.builder()
                .id(domain.getId()).userId(domain.getUserId()).accountNumber(domain.getAccountNumber())
                .caseNumber(domain.getCaseNumber()).caseType(domain.getCaseType()).priority(domain.getPriority())
                .subject(domain.getSubject()).description(domain.getDescription()).status(domain.getStatus())
                .notes(domain.getNotes()).assignedTo(domain.getAssignedTo()).resolvedBy(domain.getResolvedBy())
                .resolvedAt(domain.getResolvedAt()).createdAt(domain.getCreatedAt()).version(domain.getVersion())
                .build();
    }
}
