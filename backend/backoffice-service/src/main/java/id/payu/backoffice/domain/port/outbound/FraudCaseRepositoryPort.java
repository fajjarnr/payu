package id.payu.backoffice.domain.port.outbound;
import id.payu.backoffice.domain.*; import java.util.*;
public interface FraudCaseRepositoryPort {
 FraudCase save(FraudCase value); Optional<FraudCase> findById(UUID id); List<FraudCase> findByUserId(String userId);
 List<FraudCase> findByStatus(FraudCaseStatus status,int page,int size); List<FraudCase> findByRiskLevel(RiskLevel risk,int page,int size);
 List<FraudCase> findAll(int page,int size); void deleteById(UUID id);
 List<FraudCase> findByUserIdContainingIgnoreCase(String q); List<FraudCase> findByAccountNumberContainingIgnoreCase(String q); List<FraudCase> findByFraudTypeContainingIgnoreCase(String q);
}
