package id.payu.lending.adapter.external;

import id.payu.lending.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8081}")
public interface AccountClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUserById(@PathVariable UUID userId);

    @GetMapping("/api/v1/users/external/{externalId}")
    UserResponse getUserByExternalId(@PathVariable String externalId);
}
