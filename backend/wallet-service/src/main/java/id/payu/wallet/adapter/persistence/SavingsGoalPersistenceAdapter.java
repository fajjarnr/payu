package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.SavingsGoalEntity;
import id.payu.wallet.adapter.persistence.repository.SavingsGoalJpaRepository;
import id.payu.wallet.domain.model.SavingsGoal;
import id.payu.wallet.domain.model.SavingsGoalStatus;
import id.payu.wallet.domain.port.out.SavingsGoalPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SavingsGoalPersistenceAdapter implements SavingsGoalPersistencePort {

    private final SavingsGoalJpaRepository repository;

    public SavingsGoalPersistenceAdapter(SavingsGoalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SavingsGoal save(SavingsGoal savingsGoal) {
        SavingsGoalEntity entity = toEntity(savingsGoal);
        SavingsGoalEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SavingsGoal> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SavingsGoal> findByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status) {
        return repository.findByPocketIdAndStatusNot(pocketId, status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SavingsGoal> findByUserIdAndStatusNot(UUID userId, SavingsGoalStatus status) {
        return repository.findByUserIdAndStatusNot(userId, status).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SavingsGoal> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public long countByPocketIdAndStatusNot(UUID pocketId, SavingsGoalStatus status) {
        return repository.countByPocketIdAndStatusNot(pocketId, status);
    }

    private SavingsGoalEntity toEntity(SavingsGoal domain) {
        if (domain == null) return null;
        SavingsGoalEntity entity = new SavingsGoalEntity();
        entity.setId(domain.getId());
        entity.setPocketId(domain.getPocketId());
        entity.setUserId(domain.getUserId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setTargetAmount(domain.getTargetAmount());
        entity.setCurrentAmount(domain.getCurrentAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setDeadline(domain.getDeadline());
        entity.setStatus(domain.getStatus());
        entity.setIcon(domain.getIcon());
        entity.setColor(domain.getColor());
        entity.setCompletedAt(domain.getCompletedAt());
        return entity;
    }

    private SavingsGoal toDomain(SavingsGoalEntity entity) {
        if (entity == null) return null;
        return SavingsGoal.builder()
                .id(entity.getId())
                .pocketId(entity.getPocketId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .description(entity.getDescription())
                .targetAmount(entity.getTargetAmount())
                .currentAmount(entity.getCurrentAmount())
                .currency(entity.getCurrency())
                .deadline(entity.getDeadline())
                .status(entity.getStatus())
                .icon(entity.getIcon())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
