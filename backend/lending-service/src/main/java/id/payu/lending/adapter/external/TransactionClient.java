package id.payu.lending.adapter.external;

import id.payu.lending.dto.TransactionResponse;
import id.payu.lending.dto.TransactionSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "transaction-service", url = "${transaction.service.url:http://localhost:8085}")
public interface TransactionClient {

    @GetMapping("/api/v1/transactions/user/{userId}")
    List<TransactionResponse> getTransactionsByUserId(@PathVariable UUID userId);

    @GetMapping("/api/v1/transactions/account/{accountId}")
    List<TransactionResponse> getTransactionsByAccountId(@PathVariable UUID accountId);

    @GetMapping("/api/v1/transactions/user/{userId}/summary")
    TransactionSummaryResponse getTransactionSummary(@PathVariable UUID userId);
}
