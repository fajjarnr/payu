package id.payu.lending.adapter.external;

import id.payu.api.common.response.ApiResponse;
import id.payu.lending.dto.TransactionSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "transaction-service", url = "${transaction.service.url:http://localhost:8085}")
public interface TransactionClient {

    @GetMapping("/api/v1/transactions/accounts/{accountId}/summary")
    ApiResponse<TransactionSummaryResponse> getTransactionSummary(@PathVariable UUID accountId);
}
