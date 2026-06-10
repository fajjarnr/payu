package id.payu.notification.application.service;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;
import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.dto.SendNotificationRequest;
import id.payu.notification.adapter.sender.EmailSender;
import id.payu.notification.adapter.sender.PushSender;
import id.payu.notification.adapter.sender.SmsSender;
import id.payu.notification.domain.NotificationStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@EnabledIfSystemProperty(named = "docker.enabled", matches = "true", disabledReason = "Integration tests require Docker for Kafka DevServices and PostgreSQL")
@DisplayName("NotificationEntity Service Unit Tests")
class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @InjectMock
    EmailSender emailSender;

    @InjectMock
    SmsSender smsSender;

    @InjectMock
    PushSender pushSender;

    @Nested
    @DisplayName("Send NotificationEntity Tests")
    class SendNotificationTests {

        @Test
        @DisplayName("should send email notification successfully")
        void shouldSendEmailNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test body content",
                null,
                null,
                null
            );

            when(emailSender.send(any())).thenReturn(true);

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals("user-123", notification.userId);
            assertEquals(NotificationChannel.EMAIL, notification.channel);
            assertEquals("user@example.com", notification.recipient);
            assertEquals("Test Subject", notification.title);
            assertEquals("Test body content", notification.body);
            assertEquals(NotificationStatus.SENT, notification.status);
            assertNotNull(notification.sentAt);

            verify(emailSender).send(any());
            verifyNoInteractions(smsSender);
            verifyNoInteractions(pushSender);
        }

        @Test
        @DisplayName("should send SMS notification successfully")
        void shouldSendSmsNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.SMS,
                "+6281234567890",
                "OTP",
                "Your OTP is 123456",
                null,
                null,
                null
            );

            when(smsSender.send(any())).thenReturn(true);

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals(NotificationChannel.SMS, notification.channel);
            assertEquals("+6281234567890", notification.recipient);
            assertEquals(NotificationStatus.SENT, notification.status);

            verify(smsSender).send(any());
            verifyNoInteractions(emailSender);
            verifyNoInteractions(pushSender);
        }

        @Test
        @DisplayName("should send push notification successfully")
        void shouldSendPushNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.PUSH,
                "device-token-xyz",
                "New Transaction",
                "You received Rp 100.000",
                null,
                null,
                null
            );

            when(pushSender.send(any())).thenReturn(true);

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals(NotificationChannel.PUSH, notification.channel);
            assertEquals("device-token-xyz", notification.recipient);
            assertEquals(NotificationStatus.SENT, notification.status);

            verify(pushSender).send(any());
            verifyNoInteractions(emailSender);
            verifyNoInteractions(smsSender);
        }

        @Test
        @DisplayName("should mark notification as failed when sender returns false")
        void shouldMarkNotificationAsFailedWhenSenderFails() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test body",
                null,
                null,
                null
            );

            when(emailSender.send(any())).thenReturn(false);

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals(NotificationStatus.FAILED, notification.status);
            assertEquals("Send failed", notification.failureReason);
            assertNull(notification.sentAt);
        }

        @Test
        @DisplayName("should handle sender exception and mark as failed")
        void shouldHandleSenderExceptionAndMarkAsFailed() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.EMAIL,
                "user@example.com",
                "Test Subject",
                "Test body",
                null,
                null,
                null
            );

            when(emailSender.send(any())).thenThrow(new RuntimeException("SMTP connection failed"));

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals(NotificationStatus.FAILED, notification.status);
            assertEquals("SMTP connection failed", notification.failureReason);
            assertEquals(1, notification.retryCount);
        }

        @Test
        @DisplayName("should send in-app notification using push sender")
        void shouldSendInAppNotificationUsingPushSender() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest(
                "user-123",
                NotificationChannel.IN_APP,
                "user-device-id",
                "Promo Alert",
                "Get 50% cashback!",
                null,
                null,
                null
            );

            when(pushSender.send(any())).thenReturn(true);

            // When
            NotificationEntity notification = notificationService.send(request, null);

            // Then
            assertNotNull(notification);
            assertEquals(NotificationChannel.IN_APP, notification.channel);
            assertEquals(NotificationStatus.SENT, notification.status);

            verify(pushSender).send(any());
        }
    }

    @Nested
    @DisplayName("Get Notifications Tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("should return empty list for unknown user")
        void shouldReturnEmptyListForUnknownUser() {
            // When
            List<NotificationEntity> notifications = notificationService.getByUserId("unknown-user", 10);

            // Then
            assertNotNull(notifications);
            assertTrue(notifications.isEmpty());
        }
    }

    @Nested
    @DisplayName("Send Transaction NotificationEntity Tests")
    class SendTransactionNotificationTests {

        @Test
        @DisplayName("should send transaction notification via email")
        void shouldSendTransactionNotificationViaEmail() {
            // Given
            when(emailSender.send(any())).thenReturn(true);

            // When
            notificationService.sendTransactionNotification(
                "user-123",
                "user@example.com",
                "Transaction Success",
                "Your transfer of Rp 100.000 was successful"
            );

            // Then
            verify(emailSender).send(any());
        }
    }
}
