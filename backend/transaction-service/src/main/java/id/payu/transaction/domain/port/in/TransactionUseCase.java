package id.payu.transaction.domain.port.in;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {
    InitiateTransferResponse initiateTransfer(InitiateTransferRequest request, String userId);
    Transaction getTransaction(UUID transactionId, String userId);
    List<Transaction> getAccountTransactions(UUID accountId, String userId, int page, int size);
    void processQrisPayment(ProcessQrisPaymentRequest request, String userId);
}
