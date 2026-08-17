package id.payu.transaction.domain.port.out;

import id.payu.transaction.interfaces.dto.RgsTransferRequest;
import id.payu.transaction.interfaces.dto.RgsTransferResponse;

public interface RgsServicePort {
    RgsTransferResponse initiateTransfer(RgsTransferRequest request);
    RgsTransferResponse checkStatus(String referenceNumber);
}
