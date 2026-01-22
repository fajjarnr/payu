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
                customerCase.id,
                customerCase.userId,
                customerCase.accountNumber,
                customerCase.caseNumber,
                customerCase.caseType,
                customerCase.priority,
                customerCase.subject,
                customerCase.description,
                customerCase.status,
                customerCase.notes,
                customerCase.assignedTo,
                customerCase.resolvedBy,
                customerCase.resolvedAt,
                customerCase.createdAt
        );
    }
}
