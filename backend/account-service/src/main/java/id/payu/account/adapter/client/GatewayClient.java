package id.payu.account.adapter.client;

import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for communicating with backend services through API Gateway.
 * Provides circuit breaker and retry capabilities through resilience patterns.
 */
@FeignClient(
    name = "gateway-client",
    url = "${payu.gateway.url:http://localhost:8080}"
)
public interface GatewayClient {

    /**
     * Verify NIK with Dukcapil simulator.
     * Returns detailed citizen data for comprehensive verification.
     *
     * @param request the verification request
     * @return detailed verification response
     */
    @PostMapping("/api/v1/simulator/dukcapil/verify")
    VerifyNikResponse verifyNik(@RequestBody VerifyNikRequest request);

    /**
     * Register a new user in the identity provider (Keycloak) via auth-service.
     *
     * @param request map containing username, email, password, fullName
     * @return response body from auth-service
     */
    @PostMapping("/api/v1/auth/register")
    Map<String, Object> registerIdentity(@RequestBody Map<String, String> request);

    /**
     * ACCOUNT-005: delete a provisioned IAM user (saga compensation).
     *
     * @param userId the IAM user id to remove
     */
    @DeleteMapping("/api/v1/auth/users/{userId}")
    void deleteIdentity(@PathVariable("userId") String userId);
}
