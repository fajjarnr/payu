package id.payu.transaction.domain.model;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain Transaction value object — hexagonal hex, no JPA.
 * ADR-0060 + BUG-ARCH-003 + TXN-HARDEN-002: ArchUnit forbids jakarta.persistence in domain.
 * Entity remains TransactionEntity (JPA) in adapter/persistence/entity.
 * Domain uses Money (SCALE 4, HALF_EVEN) and enums, immutable via builder.
 */
public final class Transaction {

    private final UUID id;
    private final String referenceNumber;
    private final UUID senderAccountId;
    private final UUID recipientAccountId;
    private final TransactionType type;
    private final Money amount;
    private final String description;
    private final TransactionStatus status;
    private final String failureReason;
    private final String metadata;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant completedAt;
    private final String idempotencyKey;
    private final String reservationId;
    private final Instant expiresAt;
    private final String memo;
    private final String tags;
    private final Long version;

    private Transaction(Builder b) {
        this.id = b.id;
        this.referenceNumber = b.referenceNumber;
        this.senderAccountId = b.senderAccountId;
        this.recipientAccountId = b.recipientAccountId;
        this.type = b.type;
        this.amount = b.amount;
        this.description = b.description;
        this.status = b.status;
        this.failureReason = b.failureReason;
        this.metadata = b.metadata;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.completedAt = b.completedAt;
        this.idempotencyKey = b.idempotencyKey;
        this.reservationId = b.reservationId;
        this.expiresAt = b.expiresAt;
        this.memo = b.memo;
        this.tags = b.tags;
        this.version = b.version;
    }

    public static Builder builder() { return new Builder(); }

    public UUID getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public UUID getSenderAccountId() { return senderAccountId; }
    public UUID getRecipientAccountId() { return recipientAccountId; }
    public TransactionType getType() { return type; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public TransactionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getReservationId() { return reservationId; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getMemo() { return memo; }
    public String getTags() { return tags; }
    public Long getVersion() { return version; }

    public static final class Builder {
        private UUID id;
        private String referenceNumber;
        private UUID senderAccountId;
        private UUID recipientAccountId;
        private TransactionType type;
        private Money amount;
        private String description;
        private TransactionStatus status;
        private String failureReason;
        private String metadata;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;
        private String idempotencyKey;
        private String reservationId;
        private Instant expiresAt;
        private String memo;
        private String tags;
        private Long version;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder referenceNumber(String v) { this.referenceNumber = v; return this; }
        public Builder senderAccountId(UUID v) { this.senderAccountId = v; return this; }
        public Builder recipientAccountId(UUID v) { this.recipientAccountId = v; return this; }
        public Builder type(TransactionType v) { this.type = v; return this; }
        public Builder amount(Money v) { this.amount = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder status(TransactionStatus v) { this.status = v; return this; }
        public Builder failureReason(String v) { this.failureReason = v; return this; }
        public Builder metadata(String v) { this.metadata = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }
        public Builder completedAt(Instant v) { this.completedAt = v; return this; }
        public Builder idempotencyKey(String v) { this.idempotencyKey = v; return this; }
        public Builder reservationId(String v) { this.reservationId = v; return this; }
        public Builder expiresAt(Instant v) { this.expiresAt = v; return this; }
        public Builder memo(String v) { this.memo = v; return this; }
        public Builder tags(String v) { this.tags = v; return this; }
        public Builder version(Long v) { this.version = v; return this; }

        public Transaction build() {
            if (id == null) id = UUID.randomUUID();
            if (createdAt == null) createdAt = Instant.now();
            if (updatedAt == null) updatedAt = Instant.now();
            return new Transaction(this);
        }
    }

    // ponytail: minimal mapping helper for adapter
    public static Transaction fromEntity(TransactionEntity e) {
        if (e == null) return null;
        return Transaction.builder()
                .id(e.getId())
                .referenceNumber(e.getReferenceNumber())
                .senderAccountId(e.getSenderAccountId())
                .recipientAccountId(e.getRecipientAccountId())
                .type(e.getType())
                .amount(e.getAmount())
                .description(e.getDescription())
                .status(e.getStatus())
                .failureReason(e.getFailureReason())
                .metadata(e.getMetadata())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .completedAt(e.getCompletedAt())
                .idempotencyKey(e.getIdempotencyKey())
                .reservationId(e.getReservationId())
                .expiresAt(e.getExpiresAt())
                .memo(e.getMemo())
                .tags(e.getTags())
                .build();
    }

    public TransactionEntity toEntity() {
        return TransactionEntity.builder()
                .id(this.id)
                .referenceNumber(this.referenceNumber)
                .senderAccountId(this.senderAccountId)
                .recipientAccountId(this.recipientAccountId)
                .type(this.type)
                .amount(this.amount)
                .description(this.description)
                .status(this.status)
                .failureReason(this.failureReason)
                .metadata(this.metadata)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .completedAt(this.completedAt)
                .idempotencyKey(this.idempotencyKey)
                .reservationId(this.reservationId)
                .expiresAt(this.expiresAt)
                .memo(this.memo)
                .tags(this.tags)
                .build();
    }
}
