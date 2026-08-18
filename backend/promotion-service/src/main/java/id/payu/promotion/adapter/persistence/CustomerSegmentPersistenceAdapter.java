package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.CustomerSegmentEntity;
import id.payu.promotion.adapter.persistence.repository.CustomerSegmentRepository;
import id.payu.promotion.domain.model.CustomerSegment;
import id.payu.promotion.domain.port.out.CustomerSegmentPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerSegmentPersistenceAdapter implements CustomerSegmentPersistencePort {

    private final CustomerSegmentRepository repository;
    private final CustomerSegmentPersistenceMapper mapper;

    public CustomerSegmentPersistenceAdapter(CustomerSegmentRepository repository,
                                             CustomerSegmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CustomerSegment save(CustomerSegment segment) {
        return mapper.toDomain(repository.save(mapper.toEntity(segment)));
    }

    @Override
    public Optional<CustomerSegment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CustomerSegment> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CustomerSegment> findByIsActiveTrue() {
        return repository.findByIsActiveTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
