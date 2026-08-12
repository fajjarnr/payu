package id.payu.lending.adapter.external;

import id.payu.api.common.response.ApiResponse;
import id.payu.lending.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8081}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable String userId);

    @GetMapping("/api/v1/accounts/users/{userId}/account-ids")
    List<UUID> getAccountIdsByUserId(@PathVariable String userId);
}
