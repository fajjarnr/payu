package id.payu.wallet.interfaces.dto;

/**
 * Request DTO for manual settlement override.
 */
public class ManualOverrideRequest {

    private String reason;
    private String overriddenBy;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }
}
