package id.payu.promotion.adapter.persistence;

import id.payu.promotion.adapter.persistence.entity.PromoCodeEntity;
import id.payu.promotion.adapter.persistence.repository.PromoCodeRepository;
import id.payu.promotion.domain.model.PromoCode;
import id.payu.promotion.domain.port.out.PromoCodeRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PromoCodePersistenceAdapter implements PromoCodeRepositoryPort {

    private final PromoCodeRepository repository;
    private final PromoCodePersistenceMapper mapper;

    public PromoCodePersistenceAdapter(PromoCodeRepository repository, PromoCodePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PromoCode> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public PromoCode save(PromoCode promoCode) {
        PromoCodeEntity entity = repository.findByCode(promoCode.getCode()).orElseGet(PromoCodeEntity::new);
        mapper.updateEntity(entity, promoCode);
        return mapper.toDomain(repository.save(entity));
    }

    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
