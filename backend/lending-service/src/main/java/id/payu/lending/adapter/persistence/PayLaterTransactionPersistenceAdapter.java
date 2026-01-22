package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.port.out.PayLaterTransactionPersistencePort;
import id.payu.lending.entity.PayLaterTransactionEntity;
import id.payu.lending.repository.PayLaterTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PayLaterTransactionPersistenceAdapter implements PayLaterTransactionPersistencePort {

    private final PayLaterTransactionRepository repository;

    @Override
    public PayLaterTransaction save(PayLaterTransaction transaction) {
        PayLaterTransactionEntity entity = toEntity(transaction);
        PayLaterTransactionEntity savedEntity = repository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<PayLaterTransaction> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PayLaterTransaction> findByPayLaterAccountId(UUID paylaterAccountId) {
        return repository.findByPaylaterAccountId(paylaterAccountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PayLaterTransaction> findByPayLaterAccountIdOrderByTransactionDateDesc(UUID paylaterAccountId) {
        return repository.findByPaylaterAccountIdOrderByTransactionDateDesc(paylaterAccountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PayLaterTransaction> findByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(this::toDomain);
    }

    private PayLaterTransactionEntity toEntity(PayLaterTransaction domain) {
        return PayLaterTransactionEntity.builder()
                .id(domain.getId())
                .externalId(domain.getExternalId())
                .paylaterAccountId(domain.getPayLaterAccountId())
                .type(domain.getType())
                .amount(domain.getAmount())
                .merchantName(domain.getMerchantName())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .transactionDate(domain.getTransactionDate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PayLaterTransaction toDomain(PayLaterTransactionEntity entity) {
        PayLaterTransaction domain = new PayLaterTransaction();
        domain.setId(entity.getId());
        domain.setExternalId(entity.getExternalId());
        domain.setPayLaterAccountId(entity.getPaylaterAccountId());
        domain.setType(entity.getType());
        domain.setAmount(entity.getAmount());
        domain.setMerchantName(entity.getMerchantName());
        domain.setDescription(entity.getDescription());
        domain.setStatus(entity.getStatus());
        domain.setTransactionDate(entity.getTransactionDate());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}
