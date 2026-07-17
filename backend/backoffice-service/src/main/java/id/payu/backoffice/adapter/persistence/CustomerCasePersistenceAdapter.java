package id.payu.backoffice.adapter.persistence;

import id.payu.backoffice.adapter.persistence.repository.CustomerCaseRepository;
import id.payu.backoffice.domain.CustomerCase;
import id.payu.backoffice.domain.CustomerCaseStatus;
import id.payu.backoffice.domain.Priority;
import id.payu.backoffice.domain.port.outbound.CustomerCaseRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomerCasePersistenceAdapter implements CustomerCaseRepositoryPort {
    private final CustomerCaseRepository repository;
    private final CustomerCaseMapper mapper;

    public CustomerCase save(CustomerCase value) { return mapper.toDomain(repository.save(mapper.toEntity(value))); }
    public Optional<CustomerCase> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<CustomerCase> findByCaseNumber(String number) { return repository.findByCaseNumber(number).map(mapper::toDomain); }
    public List<CustomerCase> findByUserId(String userId) { return repository.findByUserId(userId).stream().map(mapper::toDomain).toList(); }
    public List<CustomerCase> findByStatus(CustomerCaseStatus status, int page, int size) { return repository.findByStatus(status, PageRequest.of(page, size)).map(mapper::toDomain).getContent(); }
    public List<CustomerCase> findByPriority(Priority priority, int page, int size) { return repository.findByPriority(priority, PageRequest.of(page, size)).map(mapper::toDomain).getContent(); }
    public List<CustomerCase> findAll(int page, int size) { return repository.findAll(PageRequest.of(page, size)).map(mapper::toDomain).getContent(); }
    public List<CustomerCase> findByUserIdContainingIgnoreCase(String q) { return repository.findByUserIdContainingIgnoreCase(q).stream().map(mapper::toDomain).toList(); }
    public List<CustomerCase> findByAccountNumberContainingIgnoreCase(String q) { return repository.findByAccountNumberContainingIgnoreCase(q).stream().map(mapper::toDomain).toList(); }
    public List<CustomerCase> findByCaseNumberContainingIgnoreCase(String q) { return repository.findByCaseNumberContainingIgnoreCase(q).stream().map(mapper::toDomain).toList(); }
    public List<CustomerCase> findBySubjectContainingIgnoreCase(String q) { return repository.findBySubjectContainingIgnoreCase(q).stream().map(mapper::toDomain).toList(); }
    public void deleteById(UUID id) { repository.deleteById(id); }
}
