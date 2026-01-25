package id.payu.auth.service;

import id.payu.auth.dto.*;
import id.payu.auth.exception.BiometricException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.security.*;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BiometricServiceTest {

    @InjectMocks
    private BiometricService biometricService;

    private KeyPair testKeyPair;
    private String testPublicKeyString;
    private String testUsername;
    private String testDeviceId;
    private String testDeviceType;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, IllegalAccessException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        testKeyPair = keyGen.generateKeyPair();
        testPublicKeyString = Base64.getEncoder().encodeToString(testKeyPair.getPublic().getEncoded());

        testUsername = "testuser";
        testDeviceId = "device-123";
        testDeviceType = "iOS";

        setField(biometricService, "challengeExpirySeconds", 300L);
        setField(biometricService, "maxRegistrationsPerUser", 5);
    }

    private void setField(Object target, String fieldName, Object value) throws IllegalAccessException {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field not found: " + fieldName, e);
        }
    }

    @Test
    void generateChallenge_Success() {
        long beforeTime = System.currentTimeMillis();
        BiometricChallengeResponse response = biometricService.generateChallenge(testUsername, testDeviceId);

        assertNotNull(response.challenge());
        assertNotNull(response.challengeId());
        assertNotNull(response.expiresAt());
        assertTrue(response.expiresAt() > beforeTime);
        assertEquals("Challenge generated successfully", response.message());
    }

    @Test
    void registerBiometric_Success() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );

        BiometricRegistrationResponse response = biometricService.registerBiometric(request);

        assertNotNull(response.registrationId());
        assertEquals(testUsername, response.username());
        assertEquals(testDeviceId, response.deviceId());
        assertEquals(testDeviceType, response.deviceType());
        assertEquals(testPublicKeyString, response.publicKey());
        assertNotNull(response.registeredAt());
        assertEquals("Biometric registration successful", response.message());
    }

    @Test
    void registerBiometric_MaxRegistrationsExceeded_ThrowsException() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );

        // Register 5 devices for the same user (max is 5)
        for (int i = 0; i < 5; i++) {
            BiometricRegistrationRequest r = new BiometricRegistrationRequest(
                    testUsername,  // Same username
                    testPublicKeyString,
                    testDeviceId + "-" + i,  // Different device IDs
                    testDeviceType,
                    signature,
                    challenge
            );
            biometricService.registerBiometric(r);
        }

        // The 6th registration should fail
        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.registerBiometric(request));
        assertEquals("BIO_006", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Maximum"));
    }

    @Test
    void registerBiometric_DeviceAlreadyRegistered_ThrowsException() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        biometricService.registerBiometric(request);

        BiometricRegistrationRequest request2 = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );

        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.registerBiometric(request2));
        assertEquals("BIO_007", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    void authenticateWithBiometric_Success() {
        String challenge = "auth-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest regRequest = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        biometricService.registerBiometric(regRequest);

        var registration = biometricService.findRegistration(testUsername, testDeviceId);
        assertTrue(registration.isPresent());

        String authChallenge = "auth-test-challenge";
        String authSignature = createTestSignature(authChallenge);

        BiometricAuthenticationRequest authRequest = new BiometricAuthenticationRequest(
                testUsername,
                testDeviceId,
                authSignature,
                authChallenge
        );

        BiometricAuthenticationResponse response = biometricService.authenticateWithBiometric(authRequest, registration.get());

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals(testDeviceId, response.deviceId());
        assertEquals(registration.get().registrationId(), response.registrationId());
        assertEquals("Biometric authentication successful", response.message());
    }

    @Test
    void authenticateWithBiometric_InactiveRegistration_ThrowsException() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest regRequest = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        biometricService.registerBiometric(regRequest);

        // Save the registration reference before revoking (findRegistration only returns active ones)
        var registration = biometricService.findRegistration(testUsername, testDeviceId).get();
        String registrationId = registration.registrationId();

        biometricService.revokeRegistration(registrationId);

        // Create an inactive registration object to pass to the method
        var inactiveRegistration = new id.payu.auth.dto.BiometricRegistration(
                registrationId,
                testUsername,
                testDeviceId,
                testDeviceType,
                testPublicKeyString,
                registration.createdAt(),
                false  // inactive
        );

        BiometricAuthenticationRequest authRequest = new BiometricAuthenticationRequest(
                testUsername,
                testDeviceId,
                signature,
                challenge
        );

        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.authenticateWithBiometric(authRequest, inactiveRegistration));
        assertEquals("BIO_003", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("inactive"));
    }

    @Test
    void authenticateWithBiometric_InvalidSignature_ThrowsException() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest regRequest = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        biometricService.registerBiometric(regRequest);

        var registration = biometricService.findRegistration(testUsername, testDeviceId);
        assertTrue(registration.isPresent());

        String invalidSignature = Base64.getEncoder().encodeToString(new byte[32]);
        BiometricAuthenticationRequest authRequest = new BiometricAuthenticationRequest(
                testUsername,
                testDeviceId,
                invalidSignature,
                challenge
        );

        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.authenticateWithBiometric(authRequest, registration.get()));
        assertEquals("BIO_002", exception.getErrorCode());
    }

    @Test
    void findRegistration_RegistrationExists_ReturnsRegistration() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        biometricService.registerBiometric(request);

        var result = biometricService.findRegistration(testUsername, testDeviceId);

        assertTrue(result.isPresent());
        assertEquals(testUsername, result.get().username());
        assertEquals(testDeviceId, result.get().deviceId());
    }

    @Test
    void findRegistration_RegistrationNotFound_ReturnsEmpty() {
        var result = biometricService.findRegistration("nonexistent", "nonexistent-device");
        assertFalse(result.isPresent());
    }

    @Test
    void getUserRegistrations_ReturnsUserRegistrations() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        for (int i = 0; i < 3; i++) {
            String username = testUsername + "-" + i;
            BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                    username,
                    testPublicKeyString,
                    testDeviceId + "-" + i,
                    testDeviceType,
                    signature,
                    challenge
            );
            biometricService.registerBiometric(request);
        }

        var registrations = biometricService.getUserRegistrations(testUsername + "-0");

        assertEquals(1, registrations.size());
        assertTrue(registrations.stream().allMatch(r -> r.username().equals(testUsername + "-0")));
    }

    @Test
    void revokeRegistration_MakesRegistrationInactive() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        BiometricRegistrationRequest request = new BiometricRegistrationRequest(
                testUsername,
                testPublicKeyString,
                testDeviceId,
                testDeviceType,
                signature,
                challenge
        );
        BiometricRegistrationResponse regResponse = biometricService.registerBiometric(request);

        biometricService.revokeRegistration(regResponse.registrationId());

        var result = biometricService.findRegistration(testUsername, testDeviceId);
        assertFalse(result.isPresent());
    }

    private String createTestSignature(String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(testKeyPair.getPrivate());
            signature.update(data.getBytes());
            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("Failed to create test signature", e);
        }
    }
}
