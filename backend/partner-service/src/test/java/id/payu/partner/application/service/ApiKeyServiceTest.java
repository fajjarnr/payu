package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.adapter.persistence.entity.ApiKeyEntity;
import id.payu.partner.domain.ApiKeyEntity.KeyEnvironment;
import id.payu.partner.domain.ApiKeyEntity.KeyStatus;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.dto.ApiKeyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyService — API key lifecycle management.
 * Covers: creation, rotation, revocation, validation, expiry scheduler, hashing.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private PartnerEntity activePartner;
    private PartnerEntity inactivePartner;
    private ApiKeyEntity activeKey;

    @BeforeEach
    void setUp() {
        activePartner = new PartnerEntity();
        activePartner.setId(1L);
        activePartner.setName("TokoBapak");
        activePartner.setType("MERCHANT");
        activePartner.setEmail("partner@tokobapak.com");
        activePartner.setActive(true);

        inactivePartner = new PartnerEntity();
        inactivePartner.setId(2L);
        inactivePartner.setName("InactivePartner");
        inactivePartner.setType("MERCHANT");
        inactivePartner.setEmail("inactive@example.com");
        inactivePartner.setActive(false);

        activeKey = new ApiKeyEntity(activePartner, "payu_live_", "somehash64chars",
                "xK7m", KeyEnvironment.LIVE);
        activeKey.setId(10L);
        activeKey.setName("Production Key");
        activeKey.setRatePlan("standard");
        activeKey.setRateLimitRpm(100);
        activeKey.setRateLimitRpd(10000);
    }

    // ==================== Key Creation ====================

    @Nested
    @DisplayName("API Key Creation")
    class CreateApiKey {

        @Test
        @DisplayName("should create a live API key with defaults")
        void shouldCreateLiveKeyWithDefaults() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setName("My Key");
            dto.setEnvironment("LIVE");

            ApiKeyDTO result = apiKeyService.createApiKey(1L, dto);

            assertNotNull(result);
            assertEquals("My Key", result.getName());
            assertEquals("LIVE", result.getEnvironment());
            assertEquals("standard", result.getRatePlan());
            assertEquals(100, result.getRateLimitRpm());
            assertEquals(10000, result.getRateLimitRpd());
            assertNotNull(result.getApiKey(), "Plain key should be returned once at creation");
            assertTrue(result.getApiKey().startsWith("payu_live_"));
            assertEquals("payu_live_", result.getKeyPrefix());
            assertEquals("ACTIVE", result.getStatus());
        }

        @Test
        @DisplayName("should create a sandbox API key")
        void shouldCreateSandboxKey() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                saved.setId(101L);
                return saved;
            });

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setName("Test Key");
            dto.setEnvironment("SANDBOX");

            ApiKeyDTO result = apiKeyService.createApiKey(1L, dto);

            assertNotNull(result.getApiKey());
            assertTrue(result.getApiKey().startsWith("payu_test_"));
            assertEquals("payu_test_", result.getKeyPrefix());
        }

        @Test
        @DisplayName("should use custom rate plan when provided")
        void shouldUseCustomRatePlan() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                saved.setId(102L);
                return saved;
            });

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setName("Enterprise Key");
            dto.setEnvironment("LIVE");
            dto.setRatePlan("enterprise");
            dto.setRateLimitRpm(1000);
            dto.setRateLimitRpd(100000);

            ApiKeyDTO result = apiKeyService.createApiKey(1L, dto);

            assertEquals("enterprise", result.getRatePlan());
            assertEquals(1000, result.getRateLimitRpm());
            assertEquals(100000, result.getRateLimitRpd());
        }

        @Test
        @DisplayName("should fail when max keys per partner reached")
        void shouldFailWhenMaxKeysReached() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(5L);

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setEnvironment("LIVE");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> apiKeyService.createApiKey(1L, dto));
            assertTrue(ex.getMessage().contains("maximum"));
        }

        @Test
        @DisplayName("should fail for non-existent partner")
        void shouldFailForNonExistentPartner() {
            when(partnerRepository.findById(999L)).thenReturn(Optional.empty());

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setEnvironment("LIVE");

            assertThrows(IllegalArgumentException.class,
                    () -> apiKeyService.createApiKey(999L, dto));
        }

        @Test
        @DisplayName("should fail for inactive partner")
        void shouldFailForInactivePartner() {
            when(partnerRepository.findById(2L)).thenReturn(Optional.of(inactivePartner));

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setEnvironment("LIVE");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> apiKeyService.createApiKey(2L, dto));
            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("should default to LIVE when environment is null")
        void shouldDefaultToLiveWhenEnvNull() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                saved.setId(103L);
                return saved;
            });

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setName("Null Env Key");
            // environment intentionally null

            ApiKeyDTO result = apiKeyService.createApiKey(1L, dto);

            assertEquals("LIVE", result.getEnvironment());
            assertTrue(result.getApiKey().startsWith("payu_live_"));
        }

        @Test
        @DisplayName("should store key suffix (last 4 chars)")
        void shouldStoreKeySuffix() {
            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.countByPartnerIdAndStatusIn(eq(1L), anyList())).thenReturn(0L);
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                saved.setId(104L);
                return saved;
            });

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setEnvironment("LIVE");

            ApiKeyDTO result = apiKeyService.createApiKey(1L, dto);

            assertNotNull(result.getKeySuffix());
            assertEquals(4, result.getKeySuffix().length());
            // Suffix must match last 4 chars of the plain key
            String plainKey = result.getApiKey();
            assertEquals(plainKey.substring(plainKey.length() - 4), result.getKeySuffix());
        }
    }

    // ==================== Key Rotation ====================

    @Nested
    @DisplayName("API Key Rotation")
    class RotateApiKey {

        @Test
        @DisplayName("should rotate active key with grace period")
        void shouldRotateActiveKeyWithGracePeriod() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                if (saved.getId() == null) saved.setId(11L); // new key
                return saved;
            });

            ApiKeyDTO result = apiKeyService.rotateApiKey(1L, 10L);

            assertNotNull(result);
            assertNotNull(result.getApiKey(), "New plain key must be returned");
            assertTrue(result.getApiKey().startsWith("payu_live_"));
            assertEquals("Production Key (rotated)", result.getName());
            assertEquals("standard", result.getRatePlan());

            // Verify old key was marked rotated
            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository, times(2)).save(captor.capture());

            ApiKeyEntity oldKeySaved = captor.getAllValues().get(0);
            assertEquals(KeyStatus.ROTATED, oldKeySaved.getStatus());
            assertNotNull(oldKeySaved.getGracePeriodEndsAt());
            assertTrue(oldKeySaved.getGracePeriodEndsAt().isAfter(LocalDateTime.now().plusDays(29)));
        }

        @Test
        @DisplayName("should inherit rate plan from old key")
        void shouldInheritRatePlan() {
            activeKey.setRatePlan("premium");
            activeKey.setRateLimitRpm(500);
            activeKey.setRateLimitRpd(50000);

            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> {
                ApiKeyEntity saved = inv.getArgument(0);
                if (saved.getId() == null) saved.setId(12L);
                return saved;
            });

            ApiKeyDTO result = apiKeyService.rotateApiKey(1L, 10L);

            assertEquals("premium", result.getRatePlan());
            assertEquals(500, result.getRateLimitRpm());
            assertEquals(50000, result.getRateLimitRpd());
        }

        @Test
        @DisplayName("should fail when rotating non-active key")
        void shouldFailWhenRotatingNonActiveKey() {
            activeKey.setStatus(KeyStatus.REVOKED);
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> apiKeyService.rotateApiKey(1L, 10L));
            assertTrue(ex.getMessage().contains("ACTIVE"));
        }

        @Test
        @DisplayName("should fail when key does not belong to partner")
        void shouldFailWhenKeyDoesNotBelongToPartner() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));

            assertThrows(IllegalArgumentException.class,
                    () -> apiKeyService.rotateApiKey(999L, 10L));
        }
    }

    // ==================== Key Revocation ====================

    @Nested
    @DisplayName("API Key Revocation")
    class RevokeApiKey {

        @Test
        @DisplayName("should revoke active key with reason")
        void shouldRevokeActiveKeyWithReason() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            apiKeyService.revokeApiKey(1L, 10L, "Security breach");

            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository).save(captor.capture());

            ApiKeyEntity saved = captor.getValue();
            assertEquals(KeyStatus.REVOKED, saved.getStatus());
            assertEquals("Security breach", saved.getRevokedReason());
            assertNotNull(saved.getRevokedAt());
        }

        @Test
        @DisplayName("should use default reason when null")
        void shouldUseDefaultReasonWhenNull() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            apiKeyService.revokeApiKey(1L, 10L, null);

            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository).save(captor.capture());
            assertEquals("Manual revocation", captor.getValue().getRevokedReason());
        }

        @Test
        @DisplayName("should fail when key is already revoked")
        void shouldFailWhenAlreadyRevoked() {
            activeKey.setStatus(KeyStatus.REVOKED);
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));

            assertThrows(IllegalStateException.class,
                    () -> apiKeyService.revokeApiKey(1L, 10L, "test"));
        }

        @Test
        @DisplayName("should revoke rotated key")
        void shouldRevokeRotatedKey() {
            activeKey.setStatus(KeyStatus.ROTATED);
            activeKey.setGracePeriodEndsAt(LocalDateTime.now().plusDays(15));
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            apiKeyService.revokeApiKey(1L, 10L, "Immediate revocation");

            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository).save(captor.capture());
            assertEquals(KeyStatus.REVOKED, captor.getValue().getStatus());
        }
    }

    // ==================== Key Validation ====================

    @Nested
    @DisplayName("API Key Validation")
    class ValidateKey {

        @Test
        @DisplayName("should validate active key and record usage")
        void shouldValidateActiveKey() {
            String plainKey = "payu_live_testkey123";
            String hash = apiKeyService.hashKey(plainKey);

            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", hash,
                    "123", KeyEnvironment.LIVE);
            key.setId(20L);

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);

            assertNotNull(result);
            assertNotNull(result.getLastUsedAt());
            verify(apiKeyRepository).save(key);
        }

        @Test
        @DisplayName("should return null for unknown key")
        void shouldReturnNullForUnknownKey() {
            String plainKey = "payu_live_unknown";
            String hash = apiKeyService.hashKey(plainKey);

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.empty());

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);
            assertNull(result);
        }

        @Test
        @DisplayName("should return null for revoked key")
        void shouldReturnNullForRevokedKey() {
            String plainKey = "payu_live_revokedkey";
            String hash = apiKeyService.hashKey(plainKey);

            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", hash,
                    "dkey", KeyEnvironment.LIVE);
            key.setId(21L);
            key.revoke("test");

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);
            assertNull(result);
        }

        @Test
        @DisplayName("should validate rotated key within grace period")
        void shouldValidateRotatedKeyInGracePeriod() {
            String plainKey = "payu_live_rotatedkey";
            String hash = apiKeyService.hashKey(plainKey);

            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", hash,
                    "dkey", KeyEnvironment.LIVE);
            key.setId(22L);
            key.markRotated(30); // grace period: 30 days from now

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);
            assertNotNull(result, "Rotated key should still be valid during grace period");
        }

        @Test
        @DisplayName("should return null for rotated key past grace period")
        void shouldReturnNullForRotatedKeyPastGrace() {
            String plainKey = "payu_live_expiredrotated";
            String hash = apiKeyService.hashKey(plainKey);

            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", hash,
                    "ated", KeyEnvironment.LIVE);
            key.setId(23L);
            key.setStatus(KeyStatus.ROTATED);
            key.setGracePeriodEndsAt(LocalDateTime.now().minusDays(1)); // expired grace

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);
            assertNull(result, "Rotated key past grace period should not validate");
        }

        @Test
        @DisplayName("should return null for expired key")
        void shouldReturnNullForExpiredKey() {
            String plainKey = "payu_live_expiredkey";
            String hash = apiKeyService.hashKey(plainKey);

            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", hash,
                    "dkey", KeyEnvironment.LIVE);
            key.setId(24L);
            key.setExpiresAt(LocalDateTime.now().minusDays(1));

            when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

            ApiKeyEntity result = apiKeyService.validateKey(plainKey);
            assertNull(result, "Expired key should not validate");
        }
    }

    // ==================== Key Hashing ====================

    @Nested
    @DisplayName("Key Hashing (SHA-256)")
    class KeyHashing {

        @Test
        @DisplayName("should produce consistent hash for same input")
        void shouldProduceConsistentHash() {
            String key = "payu_live_test123456";
            String hash1 = apiKeyService.hashKey(key);
            String hash2 = apiKeyService.hashKey(key);

            assertEquals(hash1, hash2);
            assertEquals(64, hash1.length(), "SHA-256 hex should be 64 chars");
        }

        @Test
        @DisplayName("should produce different hashes for different inputs")
        void shouldProduceDifferentHashes() {
            String hash1 = apiKeyService.hashKey("payu_live_key1");
            String hash2 = apiKeyService.hashKey("payu_live_key2");

            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("hash should be lowercase hex")
        void hashShouldBeLowercaseHex() {
            String hash = apiKeyService.hashKey("payu_live_testkey");
            assertTrue(hash.matches("^[0-9a-f]{64}$"),
                    "Hash should be 64 lowercase hex characters");
        }
    }

    // ==================== List & Get ====================

    @Nested
    @DisplayName("List and Get API Keys")
    class ListAndGet {

        @Test
        @DisplayName("should list all keys for partner")
        void shouldListAllKeysForPartner() {
            ApiKeyEntity key2 = new ApiKeyEntity(activePartner, "payu_test_", "hash2",
                    "ab12", KeyEnvironment.SANDBOX);
            key2.setId(11L);
            key2.setName("Sandbox Key");
            key2.setRatePlan("standard");

            when(partnerRepository.findById(1L)).thenReturn(Optional.of(activePartner));
            when(apiKeyRepository.findByPartnerId(1L)).thenReturn(List.of(activeKey, key2));

            List<ApiKeyDTO> result = apiKeyService.listApiKeys(1L);

            assertEquals(2, result.size());
            assertNull(result.get(0).getApiKey(), "Plain key should NOT be in list response");
            assertNull(result.get(1).getApiKey());
        }

        @Test
        @DisplayName("should get single key without plain text")
        void shouldGetSingleKeyWithoutPlainText() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));

            ApiKeyDTO result = apiKeyService.getApiKey(1L, 10L);

            assertEquals(10L, result.getId());
            assertEquals("Production Key", result.getName());
            assertNull(result.getApiKey(), "Plain key should never be returned on get");
        }

        @Test
        @DisplayName("should fail to get key for wrong partner")
        void shouldFailToGetKeyForWrongPartner() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));

            assertThrows(IllegalArgumentException.class,
                    () -> apiKeyService.getApiKey(999L, 10L));
        }
    }

    // ==================== Update ====================

    @Nested
    @DisplayName("Update API Key Settings")
    class UpdateApiKey {

        @Test
        @DisplayName("should update rate plan and limits")
        void shouldUpdateRatePlanAndLimits() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setRatePlan("premium");
            dto.setRateLimitRpm(500);
            dto.setRateLimitRpd(50000);

            ApiKeyDTO result = apiKeyService.updateApiKey(1L, 10L, dto);

            assertEquals("premium", result.getRatePlan());
            assertEquals(500, result.getRateLimitRpm());
            assertEquals(50000, result.getRateLimitRpd());
        }

        @Test
        @DisplayName("should only update provided fields")
        void shouldOnlyUpdateProvidedFields() {
            when(apiKeyRepository.findById(10L)).thenReturn(Optional.of(activeKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ApiKeyDTO dto = new ApiKeyDTO();
            dto.setName("Updated Name");
            // ratePlan, rpm, rpd left null

            ApiKeyDTO result = apiKeyService.updateApiKey(1L, 10L, dto);

            assertEquals("Updated Name", result.getName());
            assertEquals("standard", result.getRatePlan()); // unchanged
            assertEquals(100, result.getRateLimitRpm()); // unchanged
        }
    }

    // ==================== Expiry Scheduler ====================

    @Nested
    @DisplayName("Expiry Scheduler")
    class ExpiryScheduler {

        @Test
        @DisplayName("should expire rotated keys past grace period")
        void shouldExpireRotatedKeysPastGrace() {
            ApiKeyEntity rotatedKey = new ApiKeyEntity(activePartner, "payu_live_", "rotatedhash",
                    "rot1", KeyEnvironment.LIVE);
            rotatedKey.setId(30L);
            rotatedKey.setStatus(KeyStatus.ROTATED);
            rotatedKey.setGracePeriodEndsAt(LocalDateTime.now().minusHours(1));

            when(apiKeyRepository.findExpiredGracePeriodKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of(rotatedKey));
            when(apiKeyRepository.findExpiredKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            apiKeyService.expireRotatedKeys();

            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository).save(captor.capture());
            assertEquals(KeyStatus.EXPIRED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should expire naturally expired keys")
        void shouldExpireNaturallyExpiredKeys() {
            ApiKeyEntity expiredKey = new ApiKeyEntity(activePartner, "payu_live_", "expiredhash",
                    "exp1", KeyEnvironment.LIVE);
            expiredKey.setId(31L);
            expiredKey.setExpiresAt(LocalDateTime.now().minusDays(1));

            when(apiKeyRepository.findExpiredGracePeriodKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(apiKeyRepository.findExpiredKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of(expiredKey));
            when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            apiKeyService.expireRotatedKeys();

            ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
            verify(apiKeyRepository).save(captor.capture());
            assertEquals(KeyStatus.EXPIRED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should handle no expired keys gracefully")
        void shouldHandleNoExpiredKeys() {
            when(apiKeyRepository.findExpiredGracePeriodKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(apiKeyRepository.findExpiredKeys(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            assertDoesNotThrow(() -> apiKeyService.expireRotatedKeys());
            verify(apiKeyRepository, never()).save(any());
        }
    }

    // ==================== Domain Entity ====================

    @Nested
    @DisplayName("ApiKeyEntity Domain Methods")
    class EntityDomainMethods {

        @Test
        @DisplayName("active key should be usable")
        void activeKeyShouldBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            assertTrue(key.isUsable());
        }

        @Test
        @DisplayName("revoked key should not be usable")
        void revokedKeyShouldNotBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            key.revoke("test");
            assertFalse(key.isUsable());
            assertEquals(KeyStatus.REVOKED, key.getStatus());
            assertNotNull(key.getRevokedAt());
            assertEquals("test", key.getRevokedReason());
        }

        @Test
        @DisplayName("expired key should not be usable")
        void expiredKeyShouldNotBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            key.setStatus(KeyStatus.EXPIRED);
            assertFalse(key.isUsable());
        }

        @Test
        @DisplayName("rotated key in grace period should be usable")
        void rotatedKeyInGracePeriodShouldBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            key.markRotated(30);
            assertTrue(key.isUsable());
            assertEquals(KeyStatus.ROTATED, key.getStatus());
        }

        @Test
        @DisplayName("rotated key past grace period should not be usable")
        void rotatedKeyPastGracePeriodShouldNotBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            key.setStatus(KeyStatus.ROTATED);
            key.setGracePeriodEndsAt(LocalDateTime.now().minusDays(1));
            assertFalse(key.isUsable());
        }

        @Test
        @DisplayName("active key past expiresAt should not be usable")
        void activeKeyPastExpiresAtShouldNotBeUsable() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            key.setExpiresAt(LocalDateTime.now().minusHours(1));
            assertFalse(key.isUsable());
        }

        @Test
        @DisplayName("recordUsage should update lastUsedAt")
        void recordUsageShouldUpdateLastUsedAt() {
            ApiKeyEntity key = new ApiKeyEntity(activePartner, "payu_live_", "hash",
                    "suf1", KeyEnvironment.LIVE);
            assertNull(key.getLastUsedAt());
            key.recordUsage();
            assertNotNull(key.getLastUsedAt());
        }
    }
}
