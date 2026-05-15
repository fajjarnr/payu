package id.payu.notification.domain.port.out;

import id.payu.notification.adapter.persistence.entity.NotificationEntity;

/**
 * Outbound port for sending notifications through external channels.
 */
public interface NotificationSenderPort {

    boolean sendEmail(NotificationEntity notification);

    boolean sendSms(NotificationEntity notification);

    boolean sendPush(NotificationEntity notification);
}
