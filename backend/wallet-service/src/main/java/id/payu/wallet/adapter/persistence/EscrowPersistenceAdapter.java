package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.EscrowTransactionEntity;
import id.payu.wallet.adapter.persistence.repository.EscrowTransactionRepository;
import id.payu.wallet.domain.model.EscrowTransaction;
import id.payu.wallet.domain.port.out.EscrowPersistencePort;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter mapping EscrowTransaction domain ↔ JPA entity.
 */
@Component
public class EscrowPersistenceAdapter implements EscrowPersistencePort {

    private final EscrowTransactionRepository repository;

    public EscrowPersistenceAdapter(EscrowTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public EscrowTransaction save(EscrowTransaction escrow) {
        EscrowTransactionEntity entity = toEntity(escrow);
        EscrowTransactionEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<EscrowTransaction> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<EscrowTransaction> findByExternalReferenceId(String externalReferenceId) {
        return repository.findByExternalReferenceId(externalReferenceId).map(this::toDomain);
    }

    @Override
    public List<EscrowTransaction> findByBuyerAccountId(String buyerAccountId) {
        return repository.findByBuyerAccountIdOrderByCreatedAtDesc(buyerAccountId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EscrowTransaction> findBySellerAccountId(String sellerAccountId) {
        return repository.findBySellerAccountIdOrderByCreatedAtDesc(sellerAccountId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EscrowTransaction> findByPartnerId(String partnerId) {
        return repository.findByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EscrowTransaction> findExpiredHeldEscrows(LocalDateTime now) {
        return repository.findExpiredHeldEscrows(now)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // --- Mappers ---

    private EscrowTransactionEntity toEntity(EscrowTransaction domain) {
        EscrowTransactionEntity entity = new EscrowTransactionEntity();
        entity.setId(domain.getId());
        entity.setBuyerAccountId(domain.getBuyerAccountId());
        entity.setSellerAccountId(domain.getSellerAccountId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setAmount(domain.getAmount());
        entity.setFeeAmount(domain.getFeeAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(toEntityStatus(domain.getStatus()));
        entity.setExternalReferenceId(domain.getExternalReferenceId());
        entity.setDescription(domain.getDescription());
        entity.setReservationId(domain.getReservationId());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setHeldAt(domain.getHeldAt());
        entity.setReleasedAt(domain.getReleasedAt());
        entity.setSettledAt(domain.getSettledAt());
        entity.setRefundedAt(domain.getRefundedAt());
        entity.setRefundReason(domain.getRefundReason());
        return entity;
    }

    private EscrowTransaction toDomain(EscrowTransactionEntity entity) {
        return EscrowTransaction.builder()
                .id(entity.getId())
                .buyerAccountId(entity.getBuyerAccountId())
                .sellerAccountId(entity.getSellerAccountId())
                .partnerId(entity.getPartnerId())
                .amount(entity.getAmount())
                .feeAmount(entity.getFeeAmount())
                .currency(entity.getCurrency())
                .status(toDomainStatus(entity.getStatus()))
                .externalReferenceId(entity.getExternalReferenceId())
                .description(entity.getDescription())
                .reservationId(entity.getReservationId())
                .expiresAt(entity.getExpiresAt())
                .heldAt(entity.getHeldAt())
                .releasedAt(entity.getReleasedAt())
                .settledAt(entity.getSettledAt())
                .refundedAt(entity.getRefundedAt())
                .refundReason(entity.getRefundReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private EscrowTransactionEntity.EscrowStatus toEntityStatus(EscrowTransaction.EscrowStatus status) {
        return EscrowTransactionEntity.EscrowStatus.valueOf(status.name());
    }

    private EscrowTransaction.EscrowStatus toDomainStatus(EscrowTransactionEntity.EscrowStatus status) {
        return EscrowTransaction.EscrowStatus.valueOf(status.name());
    }
}
