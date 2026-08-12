package id.payu.notification.application.service;

import id.payu.notification.adapter.sender.EmailSender;
import id.payu.notification.adapter.sender.PushSender;
import id.payu.notification.adapter.sender.SmsSender;
import id.payu.notification.domain.Notification;
import id.payu.notification.domain.NotificationChannel;
import id.payu.notification.domain.NotificationStatus;
import id.payu.notification.domain.port.out.NotificationRepositoryPort;
import id.payu.notification.dto.SendNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMP-4 / CB-037: when the primary channel fails, the notification falls back
 * through push -> email -> SMS before being marked failed. Plain JUnit: the
 * service's @Inject fields are package-private and assigned directly.
 */
class NotificationServiceFallbackTest {

    private NotificationService service;
    private NotificationRepositoryPort repositoryPort;
    private EmailSender emailSender;
    private SmsSender smsSender;
    private PushSender pushSender;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
        repositoryPort = mock(NotificationRepositoryPort.class);
        emailSender = mock(EmailSender.class);
        smsSender = mock(SmsSender.class);
        pushSender = mock(PushSender.class);
        service.repositoryPort = repositoryPort;
        service.emailSender = emailSender;
        service.smsSender = smsSender;
        service.pushSender = pushSender;
        when(repositoryPort.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Notification sendSms(String recipient) {
        SendNotificationRequest request = new SendNotificationRequest(
                "user-1", NotificationChannel.SMS, recipient, "Title", "Body", null, null, null);
        return service.send(request, null);
    }

    @Test
    @DisplayName("SMS fails -> email fallback succeeds -> SENT")
    void fallsBackToEmailWhenSmsFails() {
        when(smsSender.send(any())).thenReturn(false);
        when(emailSender.send(any())).thenReturn(true);

        Notification result = sendSms("+6281234567890");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("SMS and email fail -> push fallback succeeds -> SENT")
    void fallsBackToPushWhenSmsAndEmailFail() {
        when(smsSender.send(any())).thenReturn(false);
        when(emailSender.send(any())).thenReturn(false);
        when(pushSender.send(any())).thenReturn(true);

        Notification result = sendSms("+6281234567890");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    @DisplayName("all channels fail -> retry scheduled (backoff), not lost")
    void failsWhenAllChannelsFail() {
        when(smsSender.send(any())).thenReturn(false);
        when(emailSender.send(any())).thenReturn(false);
        when(pushSender.send(any())).thenReturn(false);

        Notification result = sendSms("+6281234567890");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getScheduledAt()).isNotNull();
        assertThat(result.getFailureReason()).isEqualTo("Send failed");
    }

    @Test
    @DisplayName("primary success never touches fallback senders")
    void primarySuccessSkipsFallback() {
        when(smsSender.send(any())).thenReturn(true);

        Notification result = sendSms("+6281234567890");

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        org.mockito.Mockito.verify(emailSender, org.mockito.Mockito.never()).send(any());
        org.mockito.Mockito.verify(pushSender, org.mockito.Mockito.never()).send(any());
    }
}
