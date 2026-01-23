package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.SknTransferRequest;
import id.payu.transaction.dto.SknTransferResponse;

public interface SknServicePort {
    SknTransferResponse initiateTransfer(SknTransferRequest request);
    SknTransferResponse checkStatus(String referenceNumber);
}
