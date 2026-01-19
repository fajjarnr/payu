package id.payu.transaction.adapter.web;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    @PostMapping("/transfer")
    public ResponseEntity<InitiateTransferResponse> initiateTransfer(@Valid @RequestBody InitiateTransferRequest request) {
        InitiateTransferResponse response = transactionUseCase.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID transactionId) {
        Transaction transaction = transactionUseCase.getTransaction(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<Transaction>> getAccountTransactions(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Transaction> transactions = transactionUseCase.getAccountTransactions(accountId, page, size);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/qris/pay")
    public ResponseEntity<Void> processQrisPayment(@Valid @RequestBody ProcessQrisPaymentRequest request) {
        transactionUseCase.processQrisPayment(request);
        return ResponseEntity.accepted().build();
    }
}
