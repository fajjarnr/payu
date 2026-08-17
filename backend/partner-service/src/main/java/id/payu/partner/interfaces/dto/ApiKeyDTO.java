package id.payu.partner.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for API key management.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API key management")
public class ApiKeyDTO {

    @Schema(description = "Key ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Human-readable key name", example = "Production Key")
    private String name;

    @Schema(description = "Key environment", example = "LIVE")
    @Pattern(regexp = "LIVE|SANDBOX", message = "Environment must be LIVE or SANDBOX")
    private String environment;

    @Schema(description = "Rate plan name", example = "standard")
    private String ratePlan;

    @Schema(description = "Requests per minute limit", example = "100")
    private Integer rateLimitRpm;

    @Schema(description = "Requests per day limit", example = "10000")
    private Integer rateLimitRpd;

    @Schema(description = "Key prefix (read-only)", accessMode = Schema.AccessMode.READ_ONLY)
    private String keyPrefix;

    @Schema(description = "Key suffix for identification (read-only)", example = "...xK7m", accessMode = Schema.AccessMode.READ_ONLY)
    private String keySuffix;

    @Schema(description = "Key status", accessMode = Schema.AccessMode.READ_ONLY)
    private String status;

    @Schema(description = "Full API key (only returned once at creation/rotation)", accessMode = Schema.AccessMode.READ_ONLY)
    private String apiKey;

    @Schema(description = "Last used timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private String lastUsedAt;

    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt;

    @Schema(description = "When key expires (null = never)", accessMode = Schema.AccessMode.READ_ONLY)
    private String expiresAt;

    @Schema(description = "Grace period end (for rotated keys)", accessMode = Schema.AccessMode.READ_ONLY)
    private String gracePeriodEndsAt;

    @Schema(description = "Revocation reason (if revoked)", accessMode = Schema.AccessMode.READ_ONLY)
    private String revokedReason;

    public ApiKeyDTO() {}

    // --- Getters/Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getRatePlan() { return ratePlan; }
    public void setRatePlan(String ratePlan) { this.ratePlan = ratePlan; }

    public Integer getRateLimitRpm() { return rateLimitRpm; }
    public void setRateLimitRpm(Integer rateLimitRpm) { this.rateLimitRpm = rateLimitRpm; }

    public Integer getRateLimitRpd() { return rateLimitRpd; }
    public void setRateLimitRpd(Integer rateLimitRpd) { this.rateLimitRpd = rateLimitRpd; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getKeySuffix() { return keySuffix; }
    public void setKeySuffix(String keySuffix) { this.keySuffix = keySuffix; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(String lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getGracePeriodEndsAt() { return gracePeriodEndsAt; }
    public void setGracePeriodEndsAt(String gracePeriodEndsAt) { this.gracePeriodEndsAt = gracePeriodEndsAt; }

    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }
}
