package id.payu.account.adapter.client;

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

    // MESH-007: verifyNik moved to DukcapilSimulatorClient (direct call to
    // dukcapil-simulator, no longer via gateway /api/v1/simulator/dukcapil/verify).

    /**
     * Register a new user in the identity provider (Keycloak) via auth-service.
     *
     * @param request map containing username, email, password, fullName
     */
    @PostMapping("/api/v1/auth/register")
    Map<String, Object> registerIdentity(@RequestBody Map<String, String> request);

    /**
     * ACCOUNT-005: delete a provisioned IAM user (saga compensation).
     */
    @DeleteMapping("/api/v1/auth/users/{userId}")
    void deleteIdentity(@PathVariable("userId") String userId);
}
