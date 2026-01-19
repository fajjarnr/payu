package id.payu.transaction.domain.port.in;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {
    InitiateTransferResponse initiateTransfer(InitiateTransferRequest request);
    Transaction getTransaction(UUID transactionId);
    List<Transaction> getAccountTransactions(UUID accountId, int page, int size);
    void processQrisPayment(ProcessQrisPaymentRequest request);
}
