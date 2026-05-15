package id.payu.notification.domain.port.out;

import id.payu.notification.domain.Notification;

/**
 * Outbound port for sending notifications through external channels.
 */
public interface NotificationSenderPort {

    boolean sendEmail(Notification notification);

    boolean sendSms(Notification notification);

    boolean sendPush(Notification notification);
}
