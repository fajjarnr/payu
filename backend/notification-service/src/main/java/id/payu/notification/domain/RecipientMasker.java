package id.payu.notification.domain;

/**
 * Masks notification recipients for log output (PROD-045).
 * Emails keep their domain (u***@example.com), phone/NIK-like values keep the
 * last 4 digits (+628****7890). Never log title/body anywhere — they are PII.
 */
public final class RecipientMasker {

    private RecipientMasker() {
    }

    public static String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return "***";
        }
        int at = recipient.indexOf('@');
        if (at > 0) {
            String local = recipient.substring(0, at);
            String domain = recipient.substring(at);
            if (local.length() <= 2) {
                return "***" + domain;
            }
            return local.charAt(0) + "***" + domain;
        }
        String digits = recipient.replaceAll("\\D", "");
        if (digits.length() >= 8 && recipient.startsWith("+")) {
            return "+" + digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
        }
        if (digits.length() >= 6) {
            int keep = 4;
            return digits.substring(0, digits.length() - keep).replaceAll(".", "*")
                    + digits.substring(digits.length() - keep);
        }
        return "***";
    }
}
