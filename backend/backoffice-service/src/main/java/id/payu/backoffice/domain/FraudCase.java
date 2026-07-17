package id.payu.backoffice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public final class FraudCase {
    private final UUID id; private final String userId; private final String accountNumber;
    private final UUID transactionId; private final String transactionType; private final BigDecimal amount;
    private final String fraudType; private final RiskLevel riskLevel; private FraudCaseStatus status;
    private final String description; private final String evidence; private String notes; private String assignedTo;
    private String resolvedBy; private LocalDateTime resolvedAt; private final LocalDateTime createdAt; private final Long version;

    private FraudCase(UUID id,String userId,String accountNumber,UUID transactionId,String transactionType,
            BigDecimal amount,String fraudType,RiskLevel riskLevel,FraudCaseStatus status,String description,
            String evidence,String notes,String assignedTo,String resolvedBy,LocalDateTime resolvedAt,LocalDateTime createdAt,Long version) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException("amount must be non-negative");
        this.id=id; this.userId=userId; this.accountNumber=accountNumber; this.transactionId=transactionId;
        this.transactionType=transactionType; this.amount=amount.scale()>4?amount.setScale(4,RoundingMode.HALF_EVEN):amount; this.fraudType=fraudType;
        this.riskLevel=riskLevel; this.status=status; this.description=description; this.evidence=evidence; this.notes=notes;
        this.assignedTo=assignedTo; this.resolvedBy=resolvedBy; this.resolvedAt=resolvedAt; this.createdAt=createdAt; this.version=version;
    }
    public static FraudCase create(String userId,String accountNumber,UUID transactionId,String transactionType,BigDecimal amount,String fraudType,RiskLevel riskLevel,String description,String evidence) {
        return new FraudCase(null,userId,accountNumber,transactionId,transactionType,amount,fraudType,riskLevel==null?RiskLevel.MEDIUM:riskLevel,FraudCaseStatus.OPEN,description,evidence,null,null,null,null,LocalDateTime.now(),null);
    }
    public static FraudCase reconstitute(UUID id,String userId,String accountNumber,UUID transactionId,String transactionType,BigDecimal amount,String fraudType,RiskLevel riskLevel,FraudCaseStatus status,String description,String evidence,String notes,String assignedTo,String resolvedBy,LocalDateTime resolvedAt,LocalDateTime createdAt,Long version) {
        return new FraudCase(id,userId,accountNumber,transactionId,transactionType,amount,fraudType,riskLevel,status,description,evidence,notes,assignedTo,resolvedBy,resolvedAt,createdAt,version);
    }
    public void assignTo(String actor){ if(actor==null||actor.isBlank())throw new IllegalArgumentException("assignedTo is required"); assignedTo=actor; if(status==FraudCaseStatus.OPEN)status=FraudCaseStatus.UNDER_INVESTIGATION; }
    public void resolve(FraudCaseStatus next, String newNotes, String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("resolvedBy is required");
        if (status == FraudCaseStatus.RESOLVED || status == FraudCaseStatus.CLOSED) {
            throw new IllegalStateException("Terminal fraud case cannot transition");
        }
        status = next;
        notes = newNotes;
        if (next == FraudCaseStatus.RESOLVED || next == FraudCaseStatus.CLOSED) {
            resolvedBy = actor;
            resolvedAt = LocalDateTime.now();
        }
    }
    public UUID getId(){return id;} public String getUserId(){return userId;} public String getAccountNumber(){return accountNumber;}
    public UUID getTransactionId(){return transactionId;} public String getTransactionType(){return transactionType;} public BigDecimal getAmount(){return amount;}
    public String getFraudType(){return fraudType;} public RiskLevel getRiskLevel(){return riskLevel;} public FraudCaseStatus getStatus(){return status;}
    public String getDescription(){return description;} public String getEvidence(){return evidence;} public String getNotes(){return notes;}
    public String getAssignedTo(){return assignedTo;} public String getResolvedBy(){return resolvedBy;} public LocalDateTime getResolvedAt(){return resolvedAt;}
    public LocalDateTime getCreatedAt(){return createdAt;} public Long getVersion(){return version;}
}
