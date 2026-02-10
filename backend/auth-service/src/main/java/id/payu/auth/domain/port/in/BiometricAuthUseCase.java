package id.payu.auth.domain.port.in;

import id.payu.auth.domain.model.BiometricRegistration;
import id.payu.auth.dto.*;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port for biometric authentication use cases.
 */
public interface BiometricAuthUseCase {
    BiometricChallengeResponse generateChallenge(String username, String deviceId);
    BiometricRegistrationResponse registerBiometric(BiometricRegistrationRequest request);
    BiometricAuthenticationResponse authenticate(BiometricAuthenticationRequest request, BiometricRegistration registration);
    Optional<BiometricRegistration> findRegistration(String username, String deviceId);
    List<BiometricRegistration> getUserRegistrations(String username);
    void revokeRegistration(String registrationId);
}
