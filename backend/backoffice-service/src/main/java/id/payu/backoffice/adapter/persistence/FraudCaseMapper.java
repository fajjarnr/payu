package id.payu.backoffice.adapter.persistence;
import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity; import id.payu.backoffice.domain.FraudCase; import org.springframework.stereotype.Component;
@Component public class FraudCaseMapper {
 public FraudCase toDomain(FraudCaseEntity e){return FraudCase.reconstitute(e.getId(),e.getUserId(),e.getAccountNumber(),e.getTransactionId(),e.getTransactionType(),e.getAmount(),e.getFraudType(),e.getRiskLevel(),e.getStatus(),e.getDescription(),e.getEvidence(),e.getNotes(),e.getAssignedTo(),e.getResolvedBy(),e.getResolvedAt(),e.getCreatedAt(),e.getVersion());}
 public FraudCaseEntity toEntity(FraudCase d){return FraudCaseEntity.builder().id(d.getId()).userId(d.getUserId()).accountNumber(d.getAccountNumber()).transactionId(d.getTransactionId()).transactionType(d.getTransactionType()).amount(d.getAmount()).fraudType(d.getFraudType()).riskLevel(d.getRiskLevel()).status(d.getStatus()).description(d.getDescription()).evidence(d.getEvidence()).notes(d.getNotes()).assignedTo(d.getAssignedTo()).resolvedBy(d.getResolvedBy()).resolvedAt(d.getResolvedAt()).createdAt(d.getCreatedAt()).version(d.getVersion()).build();}
}
