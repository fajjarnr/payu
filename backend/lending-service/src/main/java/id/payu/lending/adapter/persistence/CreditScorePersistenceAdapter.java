package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.CreditScore;
import id.payu.lending.domain.port.out.CreditScorePersistencePort;
import id.payu.lending.entity.CreditScoreEntity;
import id.payu.lending.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class CreditScorePersistenceAdapter implements CreditScorePersistencePort {

    private final CreditScoreRepository creditScoreRepository;

    @Override
    public CreditScore save(CreditScore creditScore) {
        CreditScoreEntity entity = toEntity(creditScore);
        CreditScoreEntity saved = creditScoreRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<CreditScore> findByUserId(UUID userId) {
        return creditScoreRepository.findByUserId(userId).map(this::toDomain);
    }

    private CreditScore toDomain(CreditScoreEntity entity) {
        CreditScore creditScore = new CreditScore();
        creditScore.setId(entity.getId());
        creditScore.setUserId(entity.getUserId());
        creditScore.setScore(entity.getScore());
        creditScore.setRiskCategory(entity.getRiskCategory());
        creditScore.setLastCalculatedAt(entity.getLastCalculatedAt());
        creditScore.setCreatedAt(entity.getCreatedAt());
        creditScore.setUpdatedAt(entity.getUpdatedAt());
        return creditScore;
    }

    private CreditScoreEntity toEntity(CreditScore creditScore) {
        CreditScoreEntity entity = new CreditScoreEntity();
        entity.setId(creditScore.getId());
        entity.setUserId(creditScore.getUserId());
        entity.setScore(creditScore.getScore());
        entity.setRiskCategory(creditScore.getRiskCategory());
        entity.setLastCalculatedAt(creditScore.getLastCalculatedAt());
        entity.setCreatedAt(creditScore.getCreatedAt());
        entity.setUpdatedAt(creditScore.getUpdatedAt());
        return entity;
    }
}
