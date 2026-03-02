package id.payu.partner.adapter.web;

import id.payu.partner.adapter.web.BaseController;
import id.payu.partner.application.service.SandboxDataSeeder;
import id.payu.partner.application.service.SandboxDataSeeder.SandboxSeedResult;
import id.payu.partner.application.service.SandboxDataSeeder.TestBankAccount;
import id.payu.partner.application.service.SandboxDataSeeder.TestVaNumber;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for sandbox environment management.
 * Provides endpoints for seeding test data and managing sandbox scenarios.
 */
@RestController
@RequestMapping("/admin/sandbox")
@Tag(name = "Sandbox Admin", description = "Sandbox environment management APIs")
public class SandboxController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SandboxController.class);

    private final SandboxDataSeeder sandboxDataSeeder;

    public SandboxController(SandboxDataSeeder sandboxDataSeeder) {
        this.sandboxDataSeeder = sandboxDataSeeder;
    }

    /**
     * Seed all sandbox test data.
     * Creates test merchants, API keys, and other test resources.
     */
    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Seed sandbox test data",
            description = "Creates test merchants, API keys, and other test resources for sandbox testing")
    public ResponseEntity<ApiResponse<SandboxSeedResult>> seedTestData() {
        log.info("Admin requested sandbox test data seeding");

        try {
            SandboxSeedResult result = sandboxDataSeeder.seedAllTestData();

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to seed sandbox test data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("SEED_FAILED", "Failed to seed test data: " + e.getMessage()));
        }
    }

    /**
     * Get test bank accounts for sandbox testing.
     */
    @GetMapping("/test-accounts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Get test bank accounts",
            description = "Returns list of test bank accounts for sandbox testing")
    public ResponseEntity<ApiResponse<List<TestBankAccount>>> getTestBankAccounts() {
        List<TestBankAccount> accounts = sandboxDataSeeder.getTestBankAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    /**
     * Get test VA numbers for sandbox testing.
     */
    @GetMapping("/test-va")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Get test VA numbers",
            description = "Returns list of test virtual account numbers for sandbox testing")
    public ResponseEntity<ApiResponse<List<TestVaNumber>>> getTestVaNumbers() {
        List<TestVaNumber> vaNumbers = sandboxDataSeeder.getTestVaNumbers();
        return ResponseEntity.ok(ApiResponse.success(vaNumbers));
    }

    /**
     * Get sandbox scenario information.
     */
    @GetMapping("/scenarios")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Get sandbox test scenarios",
            description = "Returns available test scenarios for sandbox testing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTestScenarios() {
        Map<String, Object> scenarios = new HashMap<>();

        // Success scenarios
        scenarios.put("success_transfer", Map.of(
                "description", "Successful transfer to test account",
                "testAccount", SandboxDataSeeder.TEST_BCA_ACCOUNT,
                "testBank", "BCA",
                "expectedResult", "SUCCESS"
        ));

        scenarios.put("success_qr_payment", Map.of(
                "description", "Successful QRIS payment",
                "testMerchant", SandboxDataSeeder.TEST_MERCHANT_001,
                "expectedResult", "SUCCESS"
        ));

        // Failure scenarios
        scenarios.put("insufficient_funds", Map.of(
                "description", "Transfer with insufficient funds",
                "testAccount", SandboxDataSeeder.TEST_BNI_ACCOUNT,
                "testBank", "BNI",
                "amount", "999999999999",
                "expectedResult", "FAILED - Insufficient funds"
        ));

        scenarios.put("invalid_account", Map.of(
                "description", "Transfer to invalid account",
                "testAccount", "0000000000",
                "testBank", "BCA",
                "expectedResult", "FAILED - Invalid account"
        ));

        // Pending scenarios
        scenarios.put("pending_transfer", Map.of(
                "description", "Transfer that remains pending",
                "testAccount", SandboxDataSeeder.TEST_MANDIRI_ACCOUNT,
                "testBank", "MANDIRI",
                "expectedResult", "PENDING"
        ));

        return ResponseEntity.ok(ApiResponse.success(scenarios));
    }

    /**
     * Get sandbox environment status.
     */
    @GetMapping("/status")
    @Operation(summary = "Get sandbox status",
            description = "Returns sandbox environment status and configuration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSandboxStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("environment", "sandbox");
        status.put("domain", "payu.fajjjar.my.id");
        status.put("testMerchants", List.of(
                SandboxDataSeeder.TEST_MERCHANT_001,
                SandboxDataSeeder.TEST_MERCHANT_002,
                SandboxDataSeeder.TEST_MERCHANT_003
        ));
        status.put("testApiKey", "payu_test_sandbox_key_12345");
        status.put("features", List.of(
                "Deterministic responses",
                "Test data seeding",
                "Scenario simulation",
                "No real money movement"
        ));

        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
