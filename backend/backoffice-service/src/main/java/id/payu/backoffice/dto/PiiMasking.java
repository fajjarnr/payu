package id.payu.backoffice.dto;

public final class PiiMasking {
    private PiiMasking() {}

    public static String lastFour(String value) {
        if (value == null || value.isBlank()) return value;
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }

    public static String name(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1) + "****";
    }

    public static String redact(String value) {
        return value == null || value.isBlank() ? value : "[REDACTED]";
    }
}
