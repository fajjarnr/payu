package id.payu.auth.application.service;

import id.payu.auth.adapter.persistence.repository.BiometricRegistrationRepository;
import id.payu.auth.dto.*;
import id.payu.auth.exception.BiometricException;
import id.payu.cache.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.security.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiometricServiceTest {

    @Mock
    private BiometricRegistrationRepository biometricRepository;

    @Mock
    private CacheService cacheService;

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
        // Given
        long beforeTime = System.currentTimeMillis();

        // When
        BiometricChallengeResponse response = biometricService.generateChallenge(testUsername, testDeviceId);

        // Then
        assertNotNull(response.challenge());
        assertNotNull(response.challengeId());
        assertNotNull(response.expiresAt());
        assertTrue(response.expiresAt() > beforeTime);
        assertEquals("Challenge generated successfully", response.message());
        verify(cacheService).put(contains("auth:biometric:challenge:"), eq(response.challenge()), eq(Duration.ofMinutes(5)));
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

        when(biometricRepository.findByUsernameAndActiveTrue(testUsername)).thenReturn(Collections.emptyList());
        when(biometricRepository.findByUsernameAndDeviceIdAndActiveTrue(testUsername, testDeviceId)).thenReturn(Optional.empty());
        when(biometricRepository.save(any())).thenAnswer(invocation -> {
            id.payu.auth.domain.model.BiometricRegistrationEntity entity = invocation.getArgument(0);
            entity.setRegistrationId("test-registration-id");
            return entity;
        });

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

        // Create 5 existing registrations
        java.util.List<id.payu.auth.domain.model.BiometricRegistrationEntity> existingRegistrations = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            id.payu.auth.domain.model.BiometricRegistrationEntity entity = new id.payu.auth.domain.model.BiometricRegistrationEntity();
            entity.setRegistrationId("reg-" + i);
            entity.setUsername(testUsername);
            entity.setDeviceId(testDeviceId + "-" + i);
            entity.setActive(true);
            existingRegistrations.add(entity);
        }
        when(biometricRepository.findByUsernameAndActiveTrue(testUsername)).thenReturn(existingRegistrations);

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

        when(biometricRepository.findByUsernameAndActiveTrue(testUsername)).thenReturn(Collections.emptyList());

        id.payu.auth.domain.model.BiometricRegistrationEntity existingEntity = new id.payu.auth.domain.model.BiometricRegistrationEntity();
        existingEntity.setRegistrationId("existing-id");
        existingEntity.setUsername(testUsername);
        existingEntity.setDeviceId(testDeviceId);
        existingEntity.setActive(true);
        when(biometricRepository.findByUsernameAndDeviceIdAndActiveTrue(testUsername, testDeviceId))
                .thenReturn(Optional.of(existingEntity));

        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.registerBiometric(request));
        assertEquals("BIO_007", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    void authenticateWithBiometric_Success() {
        String authChallenge = "auth-test-challenge";
        String authSignature = createTestSignature(authChallenge);

        // Create a mock registration DTO
        var registration = new id.payu.auth.domain.model.BiometricRegistration(
                "test-registration-id",
                testUsername,
                testDeviceId,
                testDeviceType,
                testPublicKeyString,
                java.time.Instant.now(),
                true
        );

        BiometricAuthenticationRequest authRequest = new BiometricAuthenticationRequest(
                testUsername,
                testDeviceId,
                authSignature,
                authChallenge
        );

        BiometricAuthenticationResponse response = biometricService.authenticateWithBiometric(authRequest, registration);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(testDeviceId, response.deviceId());
        assertEquals(registration.registrationId(), response.registrationId());
        assertEquals("Biometric authentication successful", response.message());
    }

    @Test
    void authenticateWithBiometric_InactiveRegistration_ThrowsException() {
        String challenge = "test-challenge";
        String signature = createTestSignature(challenge);

        // Create an inactive registration
        var inactiveRegistration = new id.payu.auth.domain.model.BiometricRegistration(
                "test-registration-id",
                testUsername,
                testDeviceId,
                testDeviceType,
                testPublicKeyString,
                java.time.Instant.now(),
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

        // Create a mock registration
        var registration = new id.payu.auth.domain.model.BiometricRegistration(
                "test-registration-id",
                testUsername,
                testDeviceId,
                testDeviceType,
                testPublicKeyString,
                java.time.Instant.now(),
                true
        );

        String invalidSignature = Base64.getEncoder().encodeToString(new byte[32]);
        BiometricAuthenticationRequest authRequest = new BiometricAuthenticationRequest(
                testUsername,
                testDeviceId,
                invalidSignature,
                challenge
        );

        BiometricException exception = assertThrows(BiometricException.class,
                () -> biometricService.authenticateWithBiometric(authRequest, registration));
        assertEquals("BIO_002", exception.getErrorCode());
    }

    @Test
    void findRegistration_RegistrationExists_ReturnsRegistration() {
        // Given
        id.payu.auth.domain.model.BiometricRegistrationEntity entity = new id.payu.auth.domain.model.BiometricRegistrationEntity();
        entity.setRegistrationId("test-reg-id");
        entity.setUsername(testUsername);
        entity.setDeviceId(testDeviceId);
        entity.setDeviceType(testDeviceType);
        entity.setPublicKey(testPublicKeyString);
        entity.setActive(true);

        when(biometricRepository.findByUsernameAndDeviceIdAndActiveTrue(testUsername, testDeviceId))
                .thenReturn(Optional.of(entity));

        // When
        var result = biometricService.findRegistration(testUsername, testDeviceId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUsername, result.get().username());
        assertEquals(testDeviceId, result.get().deviceId());
    }

    @Test
    void findRegistration_RegistrationNotFound_ReturnsEmpty() {
        when(biometricRepository.findByUsernameAndDeviceIdAndActiveTrue("nonexistent", "nonexistent-device"))
                .thenReturn(Optional.empty());

        var result = biometricService.findRegistration("nonexistent", "nonexistent-device");
        assertFalse(result.isPresent());
    }

    @Test
    void getUserRegistrations_ReturnsUserRegistrations() {
        // Given
        java.util.List<id.payu.auth.domain.model.BiometricRegistrationEntity> mockRegistrations = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            id.payu.auth.domain.model.BiometricRegistrationEntity entity = new id.payu.auth.domain.model.BiometricRegistrationEntity();
            entity.setRegistrationId("reg-" + i);
            entity.setUsername(testUsername + "-0");
            entity.setDeviceId(testDeviceId + "-" + i);
            entity.setDeviceType(testDeviceType);
            entity.setPublicKey(testPublicKeyString);
            entity.setActive(true);
            mockRegistrations.add(entity);
        }
        when(biometricRepository.findByUsernameAndActiveTrue(testUsername + "-0")).thenReturn(mockRegistrations);

        // When
        var registrations = biometricService.getUserRegistrations(testUsername + "-0");

        // Then
        assertEquals(3, registrations.size());
        assertTrue(registrations.stream().allMatch(r -> r.username().equals(testUsername + "-0")));
    }

    @Test
    void revokeRegistration_MakesRegistrationInactive() {
        // Given
        String registrationId = "test-reg-id";
        id.payu.auth.domain.model.BiometricRegistrationEntity entity = new id.payu.auth.domain.model.BiometricRegistrationEntity();
        entity.setRegistrationId(registrationId);
        entity.setUsername(testUsername);
        entity.setDeviceId(testDeviceId);
        entity.setActive(true);

        when(biometricRepository.findById(registrationId)).thenReturn(Optional.of(entity));

        // When
        biometricService.revokeRegistration(registrationId);

        // Then
        assertFalse(entity.isActive());
        verify(biometricRepository).save(entity);
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
