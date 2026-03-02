package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.ApiKeyRepository;
import id.payu.partner.adapter.persistence.repository.MerchantRepository;
import id.payu.partner.adapter.persistence.repository.PartnerRepository;
import id.payu.partner.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for seeding test data in sandbox environment.
 * <p>
 * Provides test merchants, bank accounts, and VA numbers for partner integration testing.
 * This service is only available in sandbox/non-production environments.
 */
@Service
public class SandboxDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(SandboxDataSeeder.class);

    // Test merchant IDs
    public static final String TEST_MERCHANT_001 = "TEST-MERCHANT-001";
    public static final String TEST_MERCHANT_002 = "TEST-MERCHANT-002";
    public static final String TEST_MERCHANT_003 = "TEST-MERCHANT-003";

    // Test bank accounts (deterministic for testing)
    public static final String TEST_BCA_ACCOUNT = "1234567890";
    public static final String TEST_BNI_ACCOUNT = "0987654321";
    public static final String TEST_MANDIRI_ACCOUNT = "1122334455";
    public static final String TEST_BRI_ACCOUNT = "5566778899";

    // Test VA numbers
    public static final String TEST_VA_BCA = "1234567890123456";
    public static final String TEST_VA_BNI = "9876543210987654";
    public static final String TEST_VA_MANDIRI = "1122334455667788";

    private final PartnerRepository partnerRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;

    public SandboxDataSeeder(PartnerRepository partnerRepository,
                             MerchantRepository merchantRepository,
                             ApiKeyRepository apiKeyRepository) {
        this.partnerRepository = partnerRepository;
        this.merchantRepository = merchantRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Seed all sandbox test data.
     * Creates test partners, merchants, and API keys.
     */
    @Transactional
    public SandboxSeedResult seedAllTestData() {
        log.info("Starting sandbox test data seeding...");

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        // Seed test merchants
        seedTestMerchants(created, skipped);

        // Seed test API keys for sandbox
        seedTestApiKeys(created, skipped);

        log.info("Sandbox test data seeding completed. Created: {}, Skipped: {}",
                created.size(), skipped.size());

        return new SandboxSeedResult(created, skipped);
    }

    /**
     * Seed test merchants for sandbox testing.
     */
    private void seedTestMerchants(List<String> created, List<String> skipped) {
        // Get or create test partner
        Partner testPartner = partnerRepository.findByPartnerCode("TEST-PARTNER")
                .orElseGet(() -> {
                    Partner partner = new Partner();
                    partner.setPartnerCode("TEST-PARTNER");
                    partner.setName("Test Partner (Sandbox)");
                    partner.setEmail("sandbox@payu.fajjjar.my.id");
                    partner.setStatus(PartnerStatus.ACTIVE);
                    partner.setWebhookUrl("https://webhook.site/sandbox-test");
                    partner.setCreatedAt(LocalDateTime.now());
                    return partnerRepository.save(partner);
                });

        // Test merchant 1: Retail
        if (!merchantRepository.existsByMerchantCode(TEST_MERCHANT_001)) {
            Merchant merchant1 = new Merchant();
            merchant1.setPartner(testPartner);
            merchant1.setMerchantCode(TEST_MERCHANT_001);
            merchant1.setBusinessName("Test Retail Store");
            merchant1.setCategory(Merchant.MerchantCategory.RETAIL);
            merchant1.setStatus(Merchant.MerchantStatus.ACTIVE);
            merchant1.setSettlementAccount(TEST_BCA_ACCOUNT);
            merchant1.setSettlementBank("BCA");
            merchant1.setCreatedAt(LocalDateTime.now());
            merchantRepository.save(merchant1);
            created.add("Merchant: " + TEST_MERCHANT_001);
        } else {
            skipped.add("Merchant: " + TEST_MERCHANT_001);
        }

        // Test merchant 2: Food & Beverage
        if (!merchantRepository.existsByMerchantCode(TEST_MERCHANT_002)) {
            Merchant merchant2 = new Merchant();
            merchant2.setPartner(testPartner);
            merchant2.setMerchantCode(TEST_MERCHANT_002);
            merchant2.setBusinessName("Test Restaurant");
            merchant2.setCategory(Merchant.MerchantCategory.FOOD_BEVERAGE);
            merchant2.setStatus(Merchant.MerchantStatus.ACTIVE);
            merchant2.setSettlementAccount(TEST_BNI_ACCOUNT);
            merchant2.setSettlementBank("BNI");
            merchant2.setCreatedAt(LocalDateTime.now());
            merchantRepository.save(merchant2);
            created.add("Merchant: " + TEST_MERCHANT_002);
        } else {
            skipped.add("Merchant: " + TEST_MERCHANT_002);
        }

        // Test merchant 3: Services
        if (!merchantRepository.existsByMerchantCode(TEST_MERCHANT_003)) {
            Merchant merchant3 = new Merchant();
            merchant3.setPartner(testPartner);
            merchant3.setMerchantCode(TEST_MERCHANT_003);
            merchant3.setBusinessName("Test Service Provider");
            merchant3.setCategory(Merchant.MerchantCategory.SERVICES);
            merchant3.setStatus(Merchant.MerchantStatus.ACTIVE);
            merchant3.setSettlementAccount(TEST_MANDIRI_ACCOUNT);
            merchant3.setSettlementBank("MANDIRI");
            merchant3.setCreatedAt(LocalDateTime.now());
            merchantRepository.save(merchant3);
            created.add("Merchant: " + TEST_MERCHANT_003);
        } else {
            skipped.add("Merchant: " + TEST_MERCHANT_003);
        }
    }

    /**
     * Seed test API keys for sandbox environment.
     */
    private void seedTestApiKeys(List<String> created, List<String> skipped) {
        Partner testPartner = partnerRepository.findByPartnerCode("TEST-PARTNER")
                .orElseThrow(() -> new IllegalStateException("Test partner not found"));

        // Sandbox API Key
        String sandboxKeyHash = hashApiKey("payu_test_sandbox_key_12345");
        if (!apiKeyRepository.existsByKeyHash(sandboxKeyHash)) {
            ApiKeyEntity sandboxKey = new ApiKeyEntity(
                    testPartner,
                    "payu_test_",
                    sandboxKeyHash,
                    "2345",
                    ApiKeyEntity.KeyEnvironment.SANDBOX,
                    true
            );
            sandboxKey.setName("Sandbox Test Key");
            sandboxKey.setRatePlan("sandbox");
            sandboxKey.setRateLimitRpm(1000);
            sandboxKey.setRateLimitRpd(100000);
            apiKeyRepository.save(sandboxKey);
            created.add("API Key: payu_test_sandbox_key_12345 (SANDBOX)");
        } else {
            skipped.add("API Key: payu_test_sandbox_key_12345");
        }
    }

    /**
     * Get test bank accounts for sandbox testing.
     */
    public List<TestBankAccount> getTestBankAccounts() {
        return Arrays.asList(
                new TestBankAccount("BCA", TEST_BCA_ACCOUNT, "John Doe", "JAKARTA"),
                new TestBankAccount("BNI", TEST_BNI_ACCOUNT, "Jane Smith", "BANDUNG"),
                new TestBankAccount("MANDIRI", TEST_MANDIRI_ACCOUNT, "Bob Wilson", "SURABAYA"),
                new TestBankAccount("BRI", TEST_BRI_ACCOUNT, "Alice Brown", "YOGYAKARTA")
        );
    }

    /**
     * Get test VA numbers for sandbox testing.
     */
    public List<TestVaNumber> getTestVaNumbers() {
        return Arrays.asList(
                new TestVaNumber("BCA", TEST_VA_BCA, "Test VA BCA"),
                new TestVaNumber("BNI", TEST_VA_BNI, "Test VA BNI"),
                new TestVaNumber("MANDIRI", TEST_VA_MANDIRI, "Test VA Mandiri")
        );
    }

    /**
     * Hash API key using SHA-256.
     */
    private String hashApiKey(String apiKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    // Record classes for results
    public record SandboxSeedResult(List<String> created, List<String> skipped) {}
    public record TestBankAccount(String bankCode, String accountNumber, String accountName, String branch) {}
    public record TestVaNumber(String bankCode, String vaNumber, String description) {}
}
