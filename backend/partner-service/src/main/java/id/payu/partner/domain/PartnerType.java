package id.payu.partner.domain;

/**
 * Partner type enum — ADR-0035 Option 1.
 * INTERNAL/SANDBOX bypass dual-control for seeders and tests.
 */
public enum PartnerType {
    SNAP_BI,
    VIRTUAL_ACCOUNT,
    DISBURSEMENT,
    QRIS,
    INTERNAL,
    SANDBOX;

    public boolean isBypassDualControl() {
        return this == INTERNAL || this == SANDBOX;
    }

    public static PartnerType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        for (PartnerType t : values()) {
            if (t.name().equals(normalized)) return t;
        }
        // legacy values pass through as-is; caller decides mapping
        return null;
    }
}
