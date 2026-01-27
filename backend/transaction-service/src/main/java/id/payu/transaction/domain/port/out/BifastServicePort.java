package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.BifastTransferResponse;

import java.util.concurrent.TimeoutException;

public interface BifastServicePort {
    BifastTransferResponse initiateTransfer(BifastTransferRequest request) throws TimeoutException;
    BifastTransferResponse checkStatus(String referenceNumber);
}
