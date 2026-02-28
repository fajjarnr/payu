package id.payu.wallet.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for revenue split.
 */
public class RevenueSplitResponse {

    private UUID id;
    private String partnerId;
    private String name;
    private String description;
    private String splitType;
    private boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<StakeholderResponse> stakeholders;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDateTime effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<StakeholderResponse> getStakeholders() {
        return stakeholders;
    }

    public void setStakeholders(List<StakeholderResponse> stakeholders) {
        this.stakeholders = stakeholders;
    }
}
