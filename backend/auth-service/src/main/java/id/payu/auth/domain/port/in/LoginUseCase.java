package id.payu.auth.domain.port.in;

import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import reactor.core.publisher.Mono;

/**
 * Inbound port for authentication login use cases.
 */
public interface LoginUseCase {
    Mono<LoginResponse> login(LoginRequest request, String ipAddress, String userAgent);
}
