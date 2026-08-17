package id.payu.transaction.domain.port.in;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.interfaces.dto.CreateSplitBillRequest;
import id.payu.transaction.interfaces.dto.SplitBillResponse;
import id.payu.transaction.interfaces.dto.AddParticipantRequest;
import id.payu.transaction.interfaces.dto.MakePaymentRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SplitBillUseCase {
    SplitBillResponse createSplitBill(CreateSplitBillRequest request);

    SplitBillResponse getSplitBill(UUID splitBillId);

    List<SplitBillEntity> getAccountSplitBills(UUID accountId, int page, int size);

    SplitBillResponse updateSplitBill(UUID splitBillId, CreateSplitBillRequest request);

    SplitBillResponse cancelSplitBill(UUID splitBillId);

    SplitBillResponse activateSplitBill(UUID splitBillId);

    SplitBillResponse addParticipant(UUID splitBillId, AddParticipantRequest request);

    SplitBillResponse acceptSplitBill(UUID splitBillId, UUID participantId);

    SplitBillResponse declineSplitBill(UUID splitBillId, UUID participantId);

    SplitBillResponse makePayment(UUID splitBillId, UUID participantId, MakePaymentRequest request);

    SplitBillResponse settleSplitBill(UUID splitBillId);
}
