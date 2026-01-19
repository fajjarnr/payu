package id.payu.notification.domain;

/**
 * Notification channel types.
 */
public enum NotificationChannel {
    PUSH("Push Notification"),
    SMS("SMS"),
    EMAIL("Email"),
    IN_APP("In-App Notification");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
