package id.payu.transaction.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aggregate_results", indexes = {
        @Index(name = "idx_aggregate_results_reference_no", columnList = "reference_no")
})
public class AggregateResultEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reference_no", nullable = false, length = 64)
    private String referenceNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private String result;

    @Column(name = "fanout_order")
    private Integer fanoutOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AggregateResultEntity() {}

    public AggregateResultEntity(UUID id, String referenceNo, String result, Integer fanoutOrder, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.referenceNo = referenceNo;
        this.result = result;
        this.fanoutOrder = fanoutOrder;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Integer getFanoutOrder() { return fanoutOrder; }
    public void setFanoutOrder(Integer fanoutOrder) { this.fanoutOrder = fanoutOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
