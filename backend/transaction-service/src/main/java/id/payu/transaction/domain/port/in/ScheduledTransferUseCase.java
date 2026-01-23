package id.payu.transaction.domain.port.in;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.ScheduledTransferResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduledTransferUseCase {
    ScheduledTransferResponse createScheduledTransfer(CreateScheduledTransferRequest request);
    ScheduledTransferResponse getScheduledTransfer(UUID id);
    ScheduledTransferResponse updateScheduledTransfer(UUID id, CreateScheduledTransferRequest request);
    void cancelScheduledTransfer(UUID id);
    void pauseScheduledTransfer(UUID id);
    void resumeScheduledTransfer(UUID id);
    List<ScheduledTransfer> getAccountScheduledTransfers(UUID accountId);
}
