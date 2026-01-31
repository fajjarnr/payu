package id.payu.auth.service;

import id.payu.auth.dto.*;
import id.payu.auth.entity.BiometricRegistrationEntity;
import id.payu.auth.exception.BiometricException;
import id.payu.auth.repository.BiometricRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricRegistrationRepository biometricRepository;
    private final Map<String, String> challengeStore = new ConcurrentHashMap<>();

    @Value("${payu.biometric.challenge-expiry-seconds:300}")
    private long challengeExpirySeconds;

    @Value("${payu.biometric.max-registrations-per-user:5}")
    private int maxRegistrationsPerUser;

    public BiometricChallengeResponse generateChallenge(String username, String deviceId) {
        String challengeId = UUID.randomUUID().toString();
        String challenge = generateRandomChallenge();

        String storeKey = buildChallengeKey(username, deviceId, challengeId);
        long expiresAt = Instant.now().plusSeconds(challengeExpirySeconds).toEpochMilli();

        challengeStore.put(storeKey, challenge);

        log.info("Generated biometric challenge for user {} on device {}", username, deviceId);

        return new BiometricChallengeResponse(
                challenge,
                challengeId,
                expiresAt,
                "Challenge generated successfully"
        );
    }

    @Transactional
    public BiometricRegistrationResponse registerBiometric(BiometricRegistrationRequest request) {
        String challengeKey = buildChallengeKey(request.username(), request.deviceId(), UUID.randomUUID().toString());

        validateChallenge(request.challenge(), request.challengeSignature(), request.publicKey());
        validateDeviceLimit(request.username());
        validateDeviceUniqueness(request.username(), request.deviceId());

        String registrationId = UUID.randomUUID().toString();
        BiometricRegistrationEntity entity = new BiometricRegistrationEntity();
        entity.setRegistrationId(registrationId);
        entity.setUsername(request.username());
        entity.setDeviceId(request.deviceId());
        entity.setDeviceType(request.deviceType());
        entity.setPublicKey(request.publicKey());
        entity.setActive(true);
        // createdAt is handled by @CreationTimestamp

        biometricRepository.save(entity);

        log.info("Registered biometric for user {} on device {} with type {}",
                request.username(), request.deviceId(), request.deviceType());

        return new BiometricRegistrationResponse(
                registrationId,
                entity.getUsername(),
                entity.getDeviceId(),
                entity.getDeviceType(),
                entity.getPublicKey(),
                Instant.now(), // approximation as entity uses LocalDateTime
                "Biometric registration successful"
        );
    }

    @Transactional
    public BiometricAuthenticationResponse authenticateWithBiometric(
            BiometricAuthenticationRequest request,
            BiometricRegistrationDTO registration) { // Changed param type to simple DTO

        if (!registration.active()) {
            throw new BiometricException("BIO_003", "Biometric registration is inactive");
        }

        if (!registration.username().equals(request.username())) {
            throw new BiometricException("BIO_004", "Username mismatch");
        }

        if (!registration.deviceId().equals(request.deviceId())) {
            throw new BiometricException("BIO_005", "Device mismatch");
        }

        try {
            PublicKey publicKey = decodePublicKey(registration.publicKey());
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(request.challenge().getBytes());

            byte[] signatureBytes = Base64.getDecoder().decode(request.challengeSignature());
            boolean isValid = signature.verify(signatureBytes);

            if (!isValid) {
                throw new BiometricException("BIO_002", "Invalid biometric signature");
            }

            log.info("Successful biometric authentication for user {} on device {}",
                    request.username(), request.deviceId());

            // Access token logic...
            return new BiometricAuthenticationResponse(
                    "mock-jwt-access-token-" + UUID.randomUUID(),
                    "mock-jwt-refresh-token-" + UUID.randomUUID(),
                    "Bearer",
                    900L,
                    registration.deviceId(),
                    registration.registrationId(),
                    "Biometric authentication successful"
            );

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Biometric signature verification failed: {}", e.getMessage());
            throw new BiometricException("BIO_002", "Invalid biometric signature", e);
        }
    }

    // Helper method for controller compatibility
    public BiometricAuthenticationResponse authenticateWithBiometric(
            BiometricAuthenticationRequest request,
            BiometricRegistration registration) {
       return authenticateWithBiometric(request, new BiometricRegistrationDTO(
               registration.registrationId(),
               registration.username(),
               registration.deviceId(),
               registration.deviceType(),
               registration.publicKey(),
               registration.createdAt(),
               registration.active()
       ));
    }


    public Optional<BiometricRegistration> findRegistration(String username, String deviceId) {
        return biometricRepository.findByUsernameAndDeviceIdAndActiveTrue(username, deviceId)
                .map(this::mapToRecord);
    }

    public List<BiometricRegistration> getUserRegistrations(String username) {
        return biometricRepository.findByUsernameAndActiveTrue(username).stream()
                .map(this::mapToRecord)
                .toList();
    }

    @Transactional
    public void revokeRegistration(String registrationId) {
        biometricRepository.findById(registrationId).ifPresent(entity -> {
            entity.setActive(false);
            biometricRepository.save(entity);
            log.info("Revoked biometric registration {}", registrationId);
        });
    }

    private void validateChallenge(String challenge, String signature, String publicKey) {
        if (challenge == null || challenge.isBlank()) {
            throw new BiometricException("BIO_001", "Challenge is required");
        }
        if (signature == null || signature.isBlank()) {
            throw new BiometricException("BIO_001", "Signature is required");
        }
        if (publicKey == null || publicKey.isBlank()) {
            throw new BiometricException("BIO_001", "Public key is required");
        }
    }

    private void validateDeviceLimit(String username) {
        long activeCount = getUserRegistrations(username).size();
        if (activeCount >= maxRegistrationsPerUser) {
            throw new BiometricException("BIO_006",
                    String.format("Maximum %d biometric registrations allowed", maxRegistrationsPerUser));
        }
    }

    private void validateDeviceUniqueness(String username, String deviceId) {
        Optional<BiometricRegistration> existing = findRegistration(username, deviceId);
        if (existing.isPresent()) {
            throw new BiometricException("BIO_007",
                    String.format("Device %s already registered for user %s", deviceId, username));
        }
    }

    private String generateRandomChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String buildChallengeKey(String username, String deviceId, String challengeId) {
        return username + ":" + deviceId + ":" + challengeId;
    }

    private PublicKey decodePublicKey(String publicKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new BiometricException("BIO_001", "Invalid public key format", e);
        }
    }

    private BiometricRegistration mapToRecord(BiometricRegistrationEntity entity) {
        // Convert LocalDateTime to Instant for now to match record
        // Ideally record should use LocalDateTime too, but minimizing changes
        Instant createdInst = entity.getCreatedAt() != null 
            ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() 
            : Instant.now();
            
        return new BiometricRegistration(
                entity.getRegistrationId(),
                entity.getUsername(),
                entity.getDeviceId(),
                entity.getDeviceType(),
                entity.getPublicKey(),
                createdInst,
                entity.isActive()
        );
    }
    
    // Internal DTO to avoid circular dependency or confusion
    record BiometricRegistrationDTO(
        String registrationId,
        String username,
        String deviceId,
        String deviceType,
        String publicKey,
        Instant createdAt,
        boolean active
    ) {}
}
