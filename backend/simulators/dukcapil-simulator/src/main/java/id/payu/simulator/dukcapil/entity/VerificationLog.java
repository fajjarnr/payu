package id.payu.simulator.dukcapil.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a verification request log.
 */
@Entity
@Table(name = "verification_logs")
public class VerificationLog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "request_id", nullable = false, unique = true, length = 50)
    public String requestId;

    @Column(name = "nik", nullable = false, length = 16)
    public String nik;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false)
    public VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    public VerificationResult result;

    @Column(name = "match_score")
    public Integer matchScore;

    @Column(name = "details", length = 500)
    public String details;

    @Column(name = "client_ip", length = 50)
    public String clientIp;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public enum VerificationType {
        NIK_VERIFICATION,
        FACE_MATCHING,
        DATA_RETRIEVAL
    }

    public enum VerificationResult {
        SUCCESS,
        FAILED,
        NOT_FOUND,
        BLOCKED,
        ERROR,
        TIMEOUT
    }
}
