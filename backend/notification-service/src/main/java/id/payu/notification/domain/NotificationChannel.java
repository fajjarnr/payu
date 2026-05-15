package id.payu.notification.domain;

/**
 * NotificationEntity channel types.
 */
public enum NotificationChannel {
    PUSH("Push NotificationEntity"),
    SMS("SMS"),
    EMAIL("Email"),
    IN_APP("In-App NotificationEntity");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
