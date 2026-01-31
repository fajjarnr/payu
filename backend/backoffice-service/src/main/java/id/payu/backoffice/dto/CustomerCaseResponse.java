package id.payu.backoffice.dto;

import id.payu.backoffice.domain.CustomerCase;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerCaseResponse(
        UUID id,
        String userId,
        String accountNumber,
        String caseNumber,
        CustomerCase.CaseType caseType,
        CustomerCase.Priority priority,
        String subject,
        String description,
        CustomerCase.CaseStatus status,
        String notes,
        String assignedTo,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static CustomerCaseResponse from(CustomerCase customerCase) {
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
