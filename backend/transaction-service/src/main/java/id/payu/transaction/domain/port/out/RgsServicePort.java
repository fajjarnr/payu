package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.RgsTransferRequest;
import id.payu.transaction.dto.RgsTransferResponse;

public interface RgsServicePort {
    RgsTransferResponse initiateTransfer(RgsTransferRequest request);
    RgsTransferResponse checkStatus(String referenceNumber);
}
