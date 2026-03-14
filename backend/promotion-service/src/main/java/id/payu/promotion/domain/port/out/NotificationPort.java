package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.model.CashbackNotification;

public interface NotificationPort {
    void sendCashbackNotification(CashbackNotification notification);
}
