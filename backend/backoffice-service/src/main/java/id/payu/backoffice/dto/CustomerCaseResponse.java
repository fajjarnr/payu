package id.payu.backoffice.dto;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;

public record CustomerCaseResponse(
        UUID id,
        String userId,
        String accountNumber,
        String caseNumber,
        CaseType caseType,
        Priority priority,
        String subject,
        String description,
        CustomerCaseStatus status,
        String notes,
        String assignedTo,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static CustomerCaseResponse from(CustomerCaseEntity customerCase) {
        return new CustomerCaseResponse(
                customerCase.getId(),
                customerCase.getUserId(),
                customerCase.getAccountNumber(),
                customerCase.getCaseNumber(),
                customerCase.getCaseType(),
                customerCase.getPriority(),
                customerCase.getSubject(),
                customerCase.getDescription(),
                customerCase.getStatus(),
                customerCase.getNotes(),
                customerCase.getAssignedTo(),
                customerCase.getResolvedBy(),
                customerCase.getResolvedAt(),
                customerCase.getCreatedAt()
        );
    }
}
