package id.payu.auth.controller;

import id.payu.auth.dto.*;
import id.payu.auth.exception.BiometricException;
import id.payu.auth.service.BiometricService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiometricControllerTest {

    @Mock
    private BiometricService biometricService;

    @InjectMocks
    private BiometricController biometricController;

    private WebTestClient webTestClient;

    private KeyPair testKeyPair;
    private String testPublicKeyString;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        webTestClient = WebTestClient.bindToController(biometricController).build();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        testKeyPair = keyGen.generateKeyPair();
        testPublicKeyString = Base64.getEncoder().encodeToString(testKeyPair.getPublic().getEncoded());
    }

    @Test
    void generateChallenge_Success() {
        BiometricChallengeResponse expectedResponse = new BiometricChallengeResponse(
                "test-challenge",
                "challenge-123",
                System.currentTimeMillis() + 300000,
                "Challenge generated successfully"
        );

        when(biometricService.generateChallenge("testuser", "device-123"))
                .thenReturn(expectedResponse);

        webTestClient.get()
                .uri("/api/v1/biometric/challenge?username=testuser&deviceId=device-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BiometricChallengeResponse.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void registerBiometric_Success() {
        BiometricRegistrationResponse expectedResponse = new BiometricRegistrationResponse(
                "reg-123",
                "testuser",
                "device-123",
                "iOS",
                testPublicKeyString,
                java.time.Instant.now(),
                "Biometric registration successful"
        );

        when(biometricService.registerBiometric(any(BiometricRegistrationRequest.class)))
                .thenReturn(expectedResponse);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                "testuser",
                testPublicKeyString,
                "device-123",
                "iOS",
                "test-signature",
                "test-challenge"
        );

        webTestClient.post()
                .uri("/api/v1/biometric/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BiometricRegistrationResponse.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void registerBiometric_BiometricException_ReturnsBadRequest() {
        when(biometricService.registerBiometric(any(BiometricRegistrationRequest.class)))
                .thenThrow(new BiometricException("BIO_007", "Device already registered"));

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                "testuser",
                testPublicKeyString,
                "device-123",
                "iOS",
                "test-signature",
                "test-challenge"
        );

        webTestClient.post()
                .uri("/api/v1/biometric/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void authenticate_Success() {
        BiometricRegistration registration = new BiometricRegistration(
                "reg-123",
                "testuser",
                "device-123",
                "iOS",
                testPublicKeyString,
                java.time.Instant.now(),
                true
        );

        BiometricAuthenticationResponse expectedResponse = new BiometricAuthenticationResponse(
                "mock-access-token",
                "mock-refresh-token",
                "Bearer",
                3600L,
                "device-123",
                "reg-123",
                "Biometric authentication successful"
        );

        when(biometricService.findRegistration("testuser", "device-123"))
                .thenReturn(Optional.of(registration));
        when(biometricService.authenticateWithBiometric(any(BiometricAuthenticationRequest.class), eq(registration)))
                .thenReturn(expectedResponse);

        BiometricAuthenticationRequest request = new BiometricAuthenticationRequest(
                "testuser",
                "device-123",
                "auth-signature",
                "auth-challenge"
        );

        webTestClient.post()
                .uri("/api/v1/biometric/authenticate")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BiometricAuthenticationResponse.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void authenticate_RegistrationNotFound_ReturnsUnauthorized() {
        when(biometricService.findRegistration("testuser", "device-123"))
                .thenReturn(Optional.empty());

        BiometricAuthenticationRequest request = new BiometricAuthenticationRequest(
                "testuser",
                "device-123",
                "auth-signature",
                "auth-challenge"
        );

        webTestClient.post()
                .uri("/api/v1/biometric/authenticate")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getUserRegistrations_Success() {
        List<BiometricRegistration> registrations = List.of(
                new BiometricRegistration(
                        "reg-1",
                        "testuser",
                        "device-1",
                        "iOS",
                        testPublicKeyString,
                        java.time.Instant.now(),
                        true
                ),
                new BiometricRegistration(
                        "reg-2",
                        "testuser",
                        "device-2",
                        "Android",
                        testPublicKeyString,
                        java.time.Instant.now(),
                        true
                )
        );

        when(biometricService.getUserRegistrations("testuser"))
                .thenReturn(registrations);

        webTestClient.get()
                .uri("/api/v1/biometric/registrations/testuser")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BiometricRegistration.class)
                .hasSize(2);
    }

    @Test
    void revokeRegistration_Success() {
        doNothing().when(biometricService).revokeRegistration("reg-123");

        webTestClient.delete()
                .uri("/api/v1/biometric/registrations/reg-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Registration revoked successfully");

        verify(biometricService, times(1)).revokeRegistration("reg-123");
    }
}
