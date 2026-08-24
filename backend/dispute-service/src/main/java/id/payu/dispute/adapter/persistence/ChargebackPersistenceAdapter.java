package id.payu.dispute.adapter.persistence;

import id.payu.dispute.adapter.persistence.entity.ChargebackEntity;
import id.payu.dispute.adapter.persistence.repository.ChargebackJpaRepository;
import id.payu.dispute.domain.model.Chargeback;
import id.payu.dispute.domain.model.ChargebackStatus;
import id.payu.dispute.domain.port.out.ChargebackPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChargebackPersistenceAdapter implements ChargebackPersistencePort {

    private final ChargebackJpaRepository repository;

    @Override
    public Chargeback save(Chargeback cb) {
        ChargebackEntity entity = toEntity(cb);
        ChargebackEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Chargeback> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Chargeback> findAll() {
        return repository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Chargeback> findByStatus(ChargebackStatus status) {
        return repository.findByStatus(status).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Chargeback> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private ChargebackEntity toEntity(Chargeback d) {
        return ChargebackEntity.builder()
                .id(d.getId())
                .transactionId(d.getTransactionId())
                .customerId(d.getCustomerId())
                .merchantId(d.getMerchantId())
                .amount(d.getAmount())
                .currency(d.getCurrency())
                .reason(d.getReason())
                .status(d.getStatus())
                .schemeCaseId(d.getSchemeCaseId())
                .rejectionReason(d.getRejectionReason())
                .createdAt(d.getCreatedAt())
                .submittedAt(d.getSubmittedAt())
                .underReviewAt(d.getUnderReviewAt())
                .acceptedAt(d.getAcceptedAt())
                .rejectedAt(d.getRejectedAt())
                .reversedAt(d.getReversedAt())
                .closedAt(d.getClosedAt())
                .version(d.getVersion())
                .build();
    }

    private Chargeback toDomain(ChargebackEntity e) {
        return Chargeback.builder()
                .id(e.getId())
                .transactionId(e.getTransactionId())
                .customerId(e.getCustomerId())
                .merchantId(e.getMerchantId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .reason(e.getReason())
                .status(e.getStatus())
                .schemeCaseId(e.getSchemeCaseId())
                .rejectionReason(e.getRejectionReason())
                .createdAt(e.getCreatedAt())
                .submittedAt(e.getSubmittedAt())
                .underReviewAt(e.getUnderReviewAt())
                .acceptedAt(e.getAcceptedAt())
                .rejectedAt(e.getRejectedAt())
                .reversedAt(e.getReversedAt())
                .closedAt(e.getClosedAt())
                .version(e.getVersion())
                .build();
    }
}
