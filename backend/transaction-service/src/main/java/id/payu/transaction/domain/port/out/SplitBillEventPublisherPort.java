package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;

public interface SplitBillEventPublisherPort {
    void publishSplitBillCreated(SplitBillEntity splitBill);

    void publishSplitBillActivated(SplitBillEntity splitBill);

    void publishSplitBillCancelled(SplitBillEntity splitBill);

    void publishParticipantAdded(SplitBillEntity splitBill, SplitBillParticipantEntity participant);

    void publishPaymentMade(SplitBillEntity splitBill, SplitBillParticipantEntity participant, java.math.BigDecimal amount);

    void publishSplitBillCompleted(SplitBillEntity splitBill);

    void publishSplitBillPaymentReminder(SplitBillEntity splitBill, SplitBillParticipantEntity participant);
}
