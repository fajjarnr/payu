package id.payu.fx.adapter.persistence;

import id.payu.fx.adapter.persistence.entity.FxRateEntity;
import id.payu.fx.adapter.persistence.repository.FxRateJpaRepository;
import id.payu.fx.domain.model.FxRate;
import id.payu.fx.domain.port.out.FxRateRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FxRatePersistenceAdapter implements FxRateRepositoryPort {

    private final FxRateJpaRepository repository;

    public FxRatePersistenceAdapter(FxRateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public FxRate save(FxRate fxRate) {
        FxRateEntity entity = toEntity(fxRate);
        FxRateEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<FxRate> findLatestRate(String fromCurrency, String toCurrency, LocalDateTime timestamp) {
        return repository.findLatestValidRate(fromCurrency, toCurrency, timestamp)
                .map(this::toDomain);
    }

    @Override
    public List<FxRate> findRatesByCurrencyPair(String fromCurrency, String toCurrency) {
        return repository.findByCurrencyPair(fromCurrency, toCurrency)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<FxRate> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteExpiredRates(LocalDateTime before) {
        repository.deleteExpiredRates(before);
    }

    private FxRateEntity toEntity(FxRate fxRate) {
        FxRateEntity entity = new FxRateEntity();
        entity.setId(fxRate.getId());
        entity.setFromCurrency(fxRate.getFromCurrency());
        entity.setToCurrency(fxRate.getToCurrency());
        entity.setRate(fxRate.getRate());
        entity.setInverseRate(fxRate.getInverseRate());
        entity.setValidFrom(fxRate.getValidFrom());
        entity.setValidUntil(fxRate.getValidUntil());
        entity.setVersion(fxRate.getVersion());
        entity.setCreatedAt(fxRate.getCreatedAt());
        return entity;
    }

    private FxRate toDomain(FxRateEntity entity) {
        return FxRate.builder()
                .id(entity.getId())
                .fromCurrency(entity.getFromCurrency())
                .toCurrency(entity.getToCurrency())
                .rate(entity.getRate())
                .inverseRate(entity.getInverseRate())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
