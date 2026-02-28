package id.payu.gateway.domain.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing the assignment of a RatePlan to a Partner.
 * <p>
 * This is a linking entity that connects partners to their assigned rate plans.
 * It supports effective dates for plan changes.
 */
public class PartnerRatePlan {

    private final String id;
    private final String partnerId;
    private String ratePlanId;
    private final Instant assignedAt;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private boolean active;

    public PartnerRatePlan(String id, String partnerId, String ratePlanId) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.partnerId = Objects.requireNonNull(partnerId, "Partner ID cannot be null");
        this.ratePlanId = Objects.requireNonNull(ratePlanId, "Rate Plan ID cannot be null");
        this.assignedAt = Instant.now();
        this.effectiveFrom = this.assignedAt;
        this.effectiveUntil = null;
        this.active = true;
    }

    // Domain behavior

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changeRatePlan(String newRatePlanId) {
        this.ratePlanId = Objects.requireNonNull(newRatePlanId, "Rate Plan ID cannot be null");
    }

    public void setEffectivePeriod(Instant from, Instant until) {
        if (from != null && until != null && until.isBefore(from)) {
            throw new IllegalArgumentException("Effective until must be after effective from");
        }
        this.effectiveFrom = from;
        this.effectiveUntil = until;
    }

    public boolean isEffectiveAt(Instant timestamp) {
        if (!active) {
            return false;
        }
        if (effectiveFrom != null && timestamp.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveUntil != null && timestamp.isAfter(effectiveUntil)) {
            return false;
        }
        return true;
    }

    public boolean isEffectiveNow() {
        return isEffectiveAt(Instant.now());
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getRatePlanId() {
        return ratePlanId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveUntil() {
        return effectiveUntil;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerRatePlan that = (PartnerRatePlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
