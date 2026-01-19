package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.BifastTransferResponse;

public interface BifastServicePort {
    BifastTransferResponse initiateTransfer(BifastTransferRequest request);
    BifastTransferResponse checkStatus(String referenceNumber);
}
