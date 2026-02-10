package id.payu.auth.domain.port.in;

import id.payu.auth.dto.LoginRequest;
import id.payu.auth.dto.LoginResponse;
import id.payu.auth.dto.MFAResponse;
import reactor.core.publisher.Mono;

/**
 * Inbound port for authentication login use cases.
 */
public interface LoginUseCase {
    Mono<LoginResponse> login(LoginRequest request, String ipAddress, String userAgent);
    MFAResponse verifyMFA(String mfaToken, String otpCode);
}
