package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.repository.ReferralRepository;
import id.payu.promotion.domain.model.Referral;
import id.payu.promotion.domain.port.out.ReferralRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReferralPersistenceAdapter implements ReferralRepositoryPort {
    private final ReferralRepository repository;
    private final ReferralMapper mapper;
    public ReferralPersistenceAdapter(ReferralRepository repository, ReferralMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    public Referral save(Referral value) { return mapper.toDomain(repository.save(mapper.toEntity(value))); }
    public Optional<Referral> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }
    public Optional<Referral> findByReferralCode(String code) { return repository.findByReferralCode(code).map(mapper::toDomain); }
    public List<Referral> findByReferrerAccountId(String id) {
        return repository.findByReferrerAccountId(id).stream().map(mapper::toDomain).toList();
    }
}
