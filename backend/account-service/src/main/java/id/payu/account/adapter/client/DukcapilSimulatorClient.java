package id.payu.account.adapter.client;

import id.payu.account.interfaces.dto.VerifyNikRequest;
import id.payu.account.interfaces.dto.VerifyNikResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Dukcapil simulator, called directly by account-service.
 *
 * MESH-007 (design C): account-service no longer hops through the gateway
 * ({@code POST /api/v1/simulator/dukcapil/verify}) so the gateway proxy path
 * is no longer a public unauthenticated endpoint. The simulator serves
 * {@code POST /api/v1/verify}; in-cluster DNS resolves
 * {@code dukcapil-simulator:8080} (ClusterIP in each env namespace).
 */
@FeignClient(
    name = "dukcapil-simulator",
    url = "${payu.dukcapil.url:http://dukcapil-simulator:8080}"
)
public interface DukcapilSimulatorClient {

    /**
     * Verify NIK against Dukcapil records.
     * Returns detailed citizen data for comprehensive verification.
     */
    @PostMapping("/api/v1/verify")
    VerifyNikResponse verifyNik(@RequestBody VerifyNikRequest request);
}
