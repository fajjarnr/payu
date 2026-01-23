package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;

public interface SplitBillEventPublisherPort {
    void publishSplitBillCreated(SplitBill splitBill);

    void publishSplitBillActivated(SplitBill splitBill);

    void publishSplitBillCancelled(SplitBill splitBill);

    void publishParticipantAdded(SplitBill splitBill, SplitBillParticipant participant);

    void publishPaymentMade(SplitBill splitBill, SplitBillParticipant participant, java.math.BigDecimal amount);

    void publishSplitBillCompleted(SplitBill splitBill);

    void publishSplitBillPaymentReminder(SplitBill splitBill, SplitBillParticipant participant);
}
