package id.payu.notification;

import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.domain.Notification;
import id.payu.notification.dto.SendNotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for notification-service domain objects.
 * Verifies core domain logic without requiring Quarkus runtime.
 */
class SimpleTest {

    @Nested
    @DisplayName("NotificationChannel Enum")
    class NotificationChannelTest {

        @Test
        @DisplayName("should have all expected channels")
        void shouldHaveAllExpectedChannels() {
            NotificationChannel[] channels = NotificationChannel.values();
            assertThat(channels).hasSize(4);
            assertThat(channels).containsExactlyInAnyOrder(
                    NotificationChannel.PUSH,
                    NotificationChannel.SMS,
                    NotificationChannel.EMAIL,
                    NotificationChannel.IN_APP
            );
        }

        @Test
        @DisplayName("should have correct display names")
        void shouldHaveCorrectDisplayNames() {
            assertThat(NotificationChannel.PUSH.getDisplayName()).isEqualTo("Push Notification");
            assertThat(NotificationChannel.SMS.getDisplayName()).isEqualTo("SMS");
            assertThat(NotificationChannel.EMAIL.getDisplayName()).isEqualTo("Email");
            assertThat(NotificationChannel.IN_APP.getDisplayName()).isEqualTo("In-App Notification");
        }

        @Test
        @DisplayName("should parse from string")
        void shouldParseFromString() {
            assertThat(NotificationChannel.valueOf("PUSH")).isEqualTo(NotificationChannel.PUSH);
            assertThat(NotificationChannel.valueOf("SMS")).isEqualTo(NotificationChannel.SMS);
            assertThat(NotificationChannel.valueOf("EMAIL")).isEqualTo(NotificationChannel.EMAIL);
            assertThat(NotificationChannel.valueOf("IN_APP")).isEqualTo(NotificationChannel.IN_APP);
        }
    }

    @Nested
    @DisplayName("Notification Entity")
    class NotificationEntityTest {

        @Test
        @DisplayName("should have all expected status values")
        void shouldHaveAllExpectedStatuses() {
            Notification.NotificationStatus[] statuses = Notification.NotificationStatus.values();
            assertThat(statuses).hasSize(6);
            assertThat(statuses).containsExactlyInAnyOrder(
                    Notification.NotificationStatus.PENDING,
                    Notification.NotificationStatus.SENDING,
                    Notification.NotificationStatus.SENT,
                    Notification.NotificationStatus.DELIVERED,
                    Notification.NotificationStatus.READ,
                    Notification.NotificationStatus.FAILED
            );
        }

        @Test
        @DisplayName("should allow setting all fields")
        void shouldAllowSettingAllFields() {
            Notification notification = new Notification();
            notification.userId = "user-123";
            notification.channel = NotificationChannel.PUSH;
            notification.recipient = "device-token-abc";
            notification.title = "Test Notification";
            notification.body = "Test body";
            notification.retryCount = 3;

            assertThat(notification.userId).isEqualTo("user-123");
            assertThat(notification.channel).isEqualTo(NotificationChannel.PUSH);
            assertThat(notification.recipient).isEqualTo("device-token-abc");
            assertThat(notification.title).isEqualTo("Test Notification");
            assertThat(notification.body).isEqualTo("Test body");
            assertThat(notification.retryCount).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("SendNotificationRequest DTO")
    class SendNotificationRequestTest {

        @Test
        @DisplayName("should create valid request record")
        void shouldCreateValidRequestRecord() {
            SendNotificationRequest request = new SendNotificationRequest(
                    "user-123",
                    NotificationChannel.SMS,
                    "+6281234567890",
                    "Transfer Successful",
                    "Your transfer was successful.",
                    null,
                    null
            );

            assertThat(request.userId()).isEqualTo("user-123");
            assertThat(request.channel()).isEqualTo(NotificationChannel.SMS);
            assertThat(request.recipient()).isEqualTo("+6281234567890");
            assertThat(request.title()).isEqualTo("Transfer Successful");
            assertThat(request.body()).isEqualTo("Your transfer was successful.");
            assertThat(request.templateId()).isNull();
            assertThat(request.data()).isNull();
        }
    }
}
