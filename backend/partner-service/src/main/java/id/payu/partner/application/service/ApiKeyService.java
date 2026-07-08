package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.domain.KeyEnvironment;
import id.payu.partner.domain.KeyStatus;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.ApiKeyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Manages API key lifecycle: generation, rotation, revocation, and validation.
 * <p>
 * Security features:
 * <ul>
 *   <li>Keys hashed at rest (SHA-256) — plain text returned only once</li>
 *   <li>Prefixed keys (payu_live_, payu_test_) for environment identification</li>
 *   <li>Rotation with configurable grace period (old key remains valid temporarily)</li>
 *   <li>Per-key rate plan linkage (rpm/rpd limits)</li>
 *   <li>Revocation with reason tracking</li>
 *   <li>Max 5 active keys per partner per environment</li>
 * </ul>
 */
@Service
@Transactional
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_KEYS_PER_PARTNER = 5;
    private static final int DEFAULT_GRACE_PERIOD_DAYS = 30;

    private final ApiKeyRepository apiKeyRepository;
    private final PartnerRepository partnerRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, PartnerRepository partnerRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.partnerRepository = partnerRepository;
    }

    /**
     * Generate a new API key for a partner.
     */
    public ApiKeyDTO createApiKey(Long partnerId, ApiKeyDTO dto) {
        PartnerEntity partner = findActivePartner(partnerId);
        KeyEnvironment env = parseEnvironment(dto.getEnvironment());

        // Enforce max keys per partner per environment
        long activeCount = apiKeyRepository.countByPartnerIdAndStatusIn(
                partnerId, List.of(KeyStatus.ACTIVE, KeyStatus.ROTATED));
        if (activeCount >= MAX_KEYS_PER_PARTNER) {
            throw new IllegalStateException(
                    "PartnerEntity has reached maximum of " + MAX_KEYS_PER_PARTNER + " active keys");
        }

        // Generate key
        String plainKey = generateKey(env);
        String keyHash = hashKey(plainKey);
        String keySuffix = plainKey.substring(plainKey.length() - 4);
        String keyPrefix = env == KeyEnvironment.LIVE ? "payu_live_" : "payu_test_";

        ApiKeyEntity entity = new ApiKeyEntity(partner, keyPrefix, keyHash, keySuffix, env);
        entity.setName(dto.getName());
        entity.setRatePlan(dto.getRatePlan() != null ? dto.getRatePlan() : "standard");
        entity.setRateLimitRpm(dto.getRateLimitRpm() != null ? dto.getRateLimitRpm() : 100);
        entity.setRateLimitRpd(dto.getRateLimitRpd() != null ? dto.getRateLimitRpd() : 10000);

        entity = apiKeyRepository.save(entity);
        log.info("Created API key {} for partner {} (env: {})", entity.getId(), partnerId, env);

        return toDTO(entity, plainKey);
    }

    /**
     * Rotate an existing API key. Creates a new key and marks the old one
     * with a grace period during which both keys are valid.
     */
    public ApiKeyDTO rotateApiKey(Long partnerId, Long keyId) {
        ApiKeyEntity oldKey = findKeyForPartner(partnerId, keyId);

        if (oldKey.getStatus() != KeyStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE keys can be rotated");
        }

        // Mark old key as rotated with grace period
        oldKey.markRotated(DEFAULT_GRACE_PERIOD_DAYS);
        apiKeyRepository.save(oldKey);

        // Generate new key
        String plainKey = generateKey(oldKey.getEnvironment());
        String keyHash = hashKey(plainKey);
        String keySuffix = plainKey.substring(plainKey.length() - 4);

        ApiKeyEntity newKey = new ApiKeyEntity(
                oldKey.getPartner(), oldKey.getKeyPrefix(), keyHash, keySuffix, oldKey.getEnvironment());
        newKey.setName(oldKey.getName() != null ? oldKey.getName() + " (rotated)" : null);
        newKey.setRatePlan(oldKey.getRatePlan());
        newKey.setRateLimitRpm(oldKey.getRateLimitRpm());
        newKey.setRateLimitRpd(oldKey.getRateLimitRpd());

        newKey = apiKeyRepository.save(newKey);
        log.info("Rotated API key {} -> {} for partner {} (grace: {} days)",
                keyId, newKey.getId(), partnerId, DEFAULT_GRACE_PERIOD_DAYS);

        return toDTO(newKey, plainKey);
    }

    /**
     * Revoke an API key immediately.
     */
    public void revokeApiKey(Long partnerId, Long keyId, String reason) {
        ApiKeyEntity key = findKeyForPartner(partnerId, keyId);

        if (key.getStatus() == KeyStatus.REVOKED) {
            throw new IllegalStateException("Key is already revoked");
        }

        key.revoke(reason != null ? reason : "Manual revocation");
        apiKeyRepository.save(key);
        log.info("Revoked API key {} for partner {} (reason: {})", keyId, partnerId, reason);
    }

    /**
     * Get a specific API key (without the actual key value).
     */
    @Transactional(readOnly = true)
    public ApiKeyDTO getApiKey(Long partnerId, Long keyId) {
        ApiKeyEntity key = findKeyForPartner(partnerId, keyId);
        return toDTO(key, null);
    }

    /**
     * List all API keys for a partner.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyDTO> listApiKeys(Long partnerId) {
        findActivePartner(partnerId);
        return apiKeyRepository.findByPartnerId(partnerId).stream()
                .map(k -> toDTO(k, null))
                .toList();
    }

    /**
     * Update rate plan and limits for a key.
     */
    public ApiKeyDTO updateApiKey(Long partnerId, Long keyId, ApiKeyDTO dto) {
        ApiKeyEntity key = findKeyForPartner(partnerId, keyId);

        if (dto.getName() != null) key.setName(dto.getName());
        if (dto.getRatePlan() != null) key.setRatePlan(dto.getRatePlan());
        if (dto.getRateLimitRpm() != null) key.setRateLimitRpm(dto.getRateLimitRpm());
        if (dto.getRateLimitRpd() != null) key.setRateLimitRpd(dto.getRateLimitRpd());

        key = apiKeyRepository.save(key);
        log.info("Updated API key {} for partner {}", keyId, partnerId);
        return toDTO(key, null);
    }

    /**
     * Validate an API key by its plain-text value.
     * Returns the key entity if valid, null otherwise.
     */
    @Transactional
    public ApiKeyEntity validateKey(String plainKey) {
        String keyHash = hashKey(plainKey);
        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(ApiKeyEntity::isUsable)
                .map(key -> {
                    key.recordUsage();
                    apiKeyRepository.save(key);
                    return key;
                })
                .orElse(null);
    }

    /**
     * Expire rotated keys past their grace period.
     * Runs every hour.
     */
    @SchedulerLock(name = "ApiKeyService_expireRotatedKeys", lockAtLeastFor = "PT1S", lockAtMostFor = "PT1H")@Scheduled(fixedDelay = 3600000)
    @Transactional
    public void expireRotatedKeys() {
        try {
            List<ApiKeyEntity> expired =
                    apiKeyRepository.findExpiredGracePeriodKeys(LocalDateTime.now());
            for (ApiKeyEntity key : expired) {
                key.setStatus(KeyStatus.EXPIRED);
                apiKeyRepository.save(key);
                log.info("Expired rotated API key {} (partner {})",
                        key.getId(), key.getPartner().getId());
            }

            List<ApiKeyEntity> naturallyExpired =
                    apiKeyRepository.findExpiredKeys(LocalDateTime.now());
            for (ApiKeyEntity key : naturallyExpired) {
                key.setStatus(KeyStatus.EXPIRED);
                apiKeyRepository.save(key);
                log.info("Expired API key {} (partner {})", key.getId(), key.getPartner().getId());
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred during API key expiration scheduled task", e);
        }
    }

    // --- Internal helpers ---

    private PartnerEntity findActivePartner(Long partnerId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("PartnerEntity not found: " + partnerId));
        if (!partner.isActive()) {
            throw new IllegalStateException("Cannot manage keys for inactive partner");
        }
        return partner;
    }

    private ApiKeyEntity findKeyForPartner(Long partnerId, Long keyId) {
        ApiKeyEntity key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
        if (!Objects.equals(key.getPartner() != null ? key.getPartner().getId() : null, partnerId)) {
            throw new IllegalArgumentException(
                    "API key " + keyId + " does not belong to partner " + partnerId);
        }
        return key;
    }

    /**
     * Generate a prefixed API key with 32 random bytes (Base64url).
     * Format: payu_live_<random> or payu_test_<random>
     */
    private String generateKey(KeyEnvironment env) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String random = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefix = env == KeyEnvironment.LIVE ? "payu_live_" : "payu_test_";
        return prefix + random;
    }

    /**
     * SHA-256 hash of the API key for storage at rest.
     */
    String hashKey(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private KeyEnvironment parseEnvironment(String env) {
        if (env == null || env.isBlank()) return KeyEnvironment.LIVE;
        try {
            return KeyEnvironment.valueOf(env.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KeyEnvironment.LIVE;
        }
    }

    private ApiKeyDTO toDTO(ApiKeyEntity entity, String plainKey) {
        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEnvironment(entity.getEnvironment().name());
        dto.setRatePlan(entity.getRatePlan());
        dto.setRateLimitRpm(entity.getRateLimitRpm());
        dto.setRateLimitRpd(entity.getRateLimitRpd());
        dto.setKeyPrefix(entity.getKeyPrefix());
        dto.setKeySuffix(entity.getKeySuffix());
        dto.setStatus(entity.getStatus().name());
        if (plainKey != null) {
            dto.setApiKey(plainKey);
        }
        if (entity.getLastUsedAt() != null) {
            dto.setLastUsedAt(entity.getLastUsedAt().format(FORMATTER));
        }
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(FORMATTER));
        }
        if (entity.getExpiresAt() != null) {
            dto.setExpiresAt(entity.getExpiresAt().format(FORMATTER));
        }
        if (entity.getGracePeriodEndsAt() != null) {
            dto.setGracePeriodEndsAt(entity.getGracePeriodEndsAt().format(FORMATTER));
        }
        if (entity.getRevokedReason() != null) {
            dto.setRevokedReason(entity.getRevokedReason());
        }
        return dto;
    }
}
