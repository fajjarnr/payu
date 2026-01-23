package id.payu.billing.domain;

/**
 * Enum representing available billers.
 */
public enum BillerType {
    PLN("Listrik PLN", "PLN", "electricity"),
    PDAM("Air PDAM", "PDAM", "water"),
    TELKOMSEL("Pulsa Telkomsel", "TELKOMSEL", "mobile"),
    XL("Pulsa XL Axiata", "XL", "mobile"),
    INDOSAT("Pulsa Indosat", "INDOSAT", "mobile"),
    TRI("Pulsa Tri", "TRI", "mobile"),
    SMARTFREN("Pulsa Smartfren", "SMARTFREN", "mobile"),
    TELKOM("Telepon & Internet Telkom", "TELKOM", "internet"),
    BPJS("BPJS Kesehatan", "BPJS", "insurance"),
    PGAS("Gas PGN", "PGAS", "utility"),
    GOPAY("Top-up GoPay", "GOPAY", "ewallet"),
    OVO("Top-up OVO", "OVO", "ewallet"),
    DANA("Top-up DANA", "DANA", "ewallet"),
    LINKAJA("Top-up LinkAja", "LINKAJA", "ewallet");

    private final String displayName;
    private final String code;
    private final String category;

    BillerType(String displayName, String code, String category) {
        this.displayName = displayName;
        this.code = code;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getCategory() {
        return category;
    }
}
