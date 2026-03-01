package id.payu.statement.adapter.persistence;

import id.payu.statement.adapter.persistence.entity.ReceiptEntity;
import id.payu.statement.adapter.persistence.repository.ReceiptJpaRepository;
import id.payu.statement.application.port.output.ReceiptRepositoryPort;
import id.payu.statement.domain.model.Receipt;
import id.payu.statement.domain.model.RecipientInfo;
import id.payu.statement.domain.model.SenderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementation of ReceiptRepositoryPort.
 * Bridges the domain repository interface with JPA implementation.
 * <p>
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Component
@RequiredArgsConstructor
public class ReceiptRepositoryAdapter implements ReceiptRepositoryPort {

    private final ReceiptJpaRepository jpaRepository;

    @Override
    public Receipt save(Receipt receipt) {
        ReceiptEntity entity = toEntity(receipt);
        ReceiptEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Receipt> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Receipt> findByTransactionId(String transactionId) {
        return jpaRepository.findByTransactionId(transactionId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByTransactionId(String transactionId) {
        return jpaRepository.existsByTransactionId(transactionId);
    }

    /**
     * Convert domain Receipt to JPA Entity.
     */
    private ReceiptEntity toEntity(Receipt receipt) {
        return ReceiptEntity.builder()
                .id(receipt.getId())
                .transactionId(receipt.getTransactionId())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .senderName(receipt.getSenderInfo().getName())
                .senderAccountNumber(receipt.getSenderInfo().getAccountNumber())
                .senderBankName(receipt.getSenderInfo().getBankName())
                .recipientName(receipt.getRecipientInfo().getName())
                .recipientAccountNumber(receipt.getRecipientInfo().getAccountNumber())
                .recipientBankName(receipt.getRecipientInfo().getBankName())
                .transactionTimestamp(receipt.getTimestamp())
                .status(receipt.getStatus())
                .referenceNumber(receipt.getReferenceNumber())
                .expiryDate(receipt.getExpiryDate())
                .accessCount(receipt.getAccessCount())
                .lastAccessedAt(receipt.getLastAccessedAt())
                .createdAt(receipt.getCreatedAt())
                .build();
    }

    /**
     * Convert JPA Entity to domain Receipt.
     */
    private Receipt toDomain(ReceiptEntity entity) {
        SenderInfo senderInfo = SenderInfo.builder()
                .name(entity.getSenderName())
                .accountNumber(entity.getSenderAccountNumber())
                .bankName(entity.getSenderBankName())
                .build();

        RecipientInfo recipientInfo = RecipientInfo.builder()
                .name(entity.getRecipientName())
                .accountNumber(entity.getRecipientAccountNumber())
                .bankName(entity.getRecipientBankName())
                .build();

        return Receipt.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .senderInfo(senderInfo)
                .recipientInfo(recipientInfo)
                .timestamp(entity.getTransactionTimestamp())
                .status(entity.getStatus())
                .referenceNumber(entity.getReferenceNumber())
                .expiryDate(entity.getExpiryDate())
                .accessCount(entity.getAccessCount() != null ? entity.getAccessCount() : 0)
                .lastAccessedAt(entity.getLastAccessedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
