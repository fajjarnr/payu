package id.payu.account.service;

import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.VerifyNikRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "gateway-client", url = "${payu.gateway.url}")
public interface GatewayClient {

    @PostMapping("/api/v1/simulator/dukcapil/verify")
    DukcapilResponse verifyNik(@RequestBody VerifyNikRequest request);
}
