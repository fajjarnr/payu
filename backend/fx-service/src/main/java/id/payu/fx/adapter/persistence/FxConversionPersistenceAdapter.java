package id.payu.fx.adapter.persistence;

import id.payu.fx.adapter.persistence.entity.FxConversionEntity;
import id.payu.fx.adapter.persistence.repository.FxConversionJpaRepository;
import id.payu.fx.domain.model.FxConversion;
import id.payu.fx.domain.port.out.FxConversionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FxConversionPersistenceAdapter implements FxConversionRepositoryPort {

    private final FxConversionJpaRepository repository;

    public FxConversionPersistenceAdapter(FxConversionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public FxConversion save(FxConversion conversion) {
        FxConversionEntity entity = toEntity(conversion);
        FxConversionEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<FxConversion> findById(UUID conversionId) {
        return repository.findById(conversionId)
                .map(this::toDomain);
    }

    @Override
    public List<FxConversion> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID conversionId) {
        repository.deleteById(conversionId);
    }

    private FxConversionEntity toEntity(FxConversion conversion) {
        FxConversionEntity entity = new FxConversionEntity();
        entity.setId(conversion.getId());
        entity.setAccountId(conversion.getAccountId());
        entity.setFromCurrency(conversion.getFromCurrency());
        entity.setToCurrency(conversion.getToCurrency());
        entity.setFromAmount(conversion.getFromAmount());
        entity.setToAmount(conversion.getToAmount());
        entity.setExchangeRate(conversion.getExchangeRate());
        entity.setFee(conversion.getFee());
        entity.setConversionDate(conversion.getConversionDate());
        entity.setStatus(conversion.getStatus());
        return entity;
    }

    private FxConversion toDomain(FxConversionEntity entity) {
        return FxConversion.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .fromCurrency(entity.getFromCurrency())
                .toCurrency(entity.getToCurrency())
                .fromAmount(entity.getFromAmount())
                .toAmount(entity.getToAmount())
                .exchangeRate(entity.getExchangeRate())
                .fee(entity.getFee())
                .conversionDate(entity.getConversionDate())
                .status(entity.getStatus())
                .build();
    }
}
