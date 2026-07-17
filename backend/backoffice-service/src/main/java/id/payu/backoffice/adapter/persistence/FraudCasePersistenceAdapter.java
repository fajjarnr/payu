package id.payu.backoffice.adapter.persistence;
import id.payu.backoffice.adapter.persistence.repository.FraudCaseRepository; import id.payu.backoffice.domain.*; import id.payu.backoffice.domain.port.outbound.FraudCaseRepositoryPort; import java.util.*; import lombok.RequiredArgsConstructor; import org.springframework.data.domain.PageRequest; import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor public class FraudCasePersistenceAdapter implements FraudCaseRepositoryPort {
 private final FraudCaseRepository repository; private final FraudCaseMapper mapper;
 public FraudCase save(FraudCase v){return mapper.toDomain(repository.save(mapper.toEntity(v)));} public Optional<FraudCase> findById(UUID id){return repository.findById(id).map(mapper::toDomain);}
 public List<FraudCase> findByUserId(String id){return repository.findByUserId(id).stream().map(mapper::toDomain).toList();}
 public List<FraudCase> findByStatus(FraudCaseStatus s,int p,int z){return repository.findByStatus(s,PageRequest.of(p,z)).map(mapper::toDomain).getContent();}
 public List<FraudCase> findByRiskLevel(RiskLevel r,int p,int z){return repository.findByRiskLevel(r,PageRequest.of(p,z)).map(mapper::toDomain).getContent();}
 public List<FraudCase> findAll(int p,int z){return repository.findAll(PageRequest.of(p,z)).map(mapper::toDomain).getContent();} public void deleteById(UUID id){repository.deleteById(id);}
 public List<FraudCase> findByUserIdContainingIgnoreCase(String q){return repository.findByUserIdContainingIgnoreCase(q).stream().map(mapper::toDomain).toList();} public List<FraudCase> findByAccountNumberContainingIgnoreCase(String q){return repository.findByAccountNumberContainingIgnoreCase(q).stream().map(mapper::toDomain).toList();} public List<FraudCase> findByFraudTypeContainingIgnoreCase(String q){return repository.findByFraudTypeContainingIgnoreCase(q).stream().map(mapper::toDomain).toList();}
}
