package id.payu.account.integration;

import id.payu.account.adapter.client.GatewayClient;
import id.payu.account.adapter.client.IdentityProviderAdapter;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.application.service.UserApplicationService;
import id.payu.account.domain.model.User;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.dto.VerifyNikResponse;
import id.payu.security.multitenancy.TenantContext;
import id.payu.security.crypto.BlindIndexService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * ACCOUNT-001 + ACCOUNT-003 integration: blind-index PII lookup with real
 * PostgreSQL, and cross-tenant isolation through the enforced tenant filter.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("Blind index and tenant isolation integration")
@org.junit.jupiter.api.Tag("integration")
class BlindIndexAndTenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserApplicationService userApplicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private id.payu.account.domain.port.out.UserPersistencePort userPersistencePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BlindIndexService blindIndexService;

    @MockitoBean
    private GatewayClient gatewayClient;

    @MockitoBean
    private IdentityProviderAdapter identityProviderAdapter;

    private static final String EMAIL = "blindindex@example.com";
    private static final String PHONE = "+6281999000111";

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("email lookup works through the blind index and ciphertext stays at rest")
    void emailLookupWorksAndPlaintextNeverPersisted() throws Exception {
        User registered = register(EMAIL, PHONE, "blindindex-user");

        Optional<User> byEmail = userPersistencePort.findByEmail(EMAIL);
        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().getId()).isEqualTo(registered.getId());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT email, email_hash, phone_number_hash FROM users WHERE id = ?::uuid",
                registered.getId().toString());
        assertThat(row.get("email")).isNotEqualTo(EMAIL);
        assertThat(row.get("email_hash"))
                .isEqualTo(blindIndexService.index(EMAIL.trim().toLowerCase(Locale.ROOT)));
        assertThat(row.get("phone_number_hash"))
                .isEqualTo(blindIndexService.index(PHONE.trim()));
    }

    @Test
    @DisplayName("duplicate email is rejected in the same tenant")
    void duplicateEmailRejectedInSameTenant() throws Exception {
        register(EMAIL, "+6281999000222", "dup-user-1");
        assertThatThrownBy(() -> register(EMAIL, "+6281999000333", "dup-user-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("the same email may exist in a different tenant (tenant-scoped uniqueness)")
    void sameEmailAllowedInDifferentTenant() throws Exception {
        register(EMAIL, "+6281999000444", "tenant-a-user");
        TenantContext.setTenantId("tenant-b");
        User second = register(EMAIL, "+6281999000555", "tenant-b-user");

        assertThat(second.getId()).isNotNull();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT tenant_id FROM users WHERE id = ?::uuid", second.getId().toString());
        assertThat(row.get("tenant_id")).isEqualTo("tenant-b");
    }

    @Test
    @DisplayName("phone lookup is isolated across tenants")
    void phoneLookupIsolatedAcrossTenants() throws Exception {
        TenantContext.setTenantId("tenant-a");
        register(EMAIL, PHONE, "tenant-a-phone-user");

        TenantContext.setTenantId("tenant-b");
        assertThat(userRepository.findByPhoneNumberHash(
                blindIndexService.index(PHONE.trim()))).isEmpty();

        TenantContext.setTenantId("tenant-a");
        assertThat(userRepository.findByPhoneNumberHash(
                blindIndexService.index(PHONE.trim()))).isPresent();
    }

    private User register(String email, String phone, String username) throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                UUID.randomUUID().toString(),
                username,
                email,
                phone,
                "Blind Index Tester",
                "3201234567890002",
                "SecureP@ss123"
        );
        given(identityProviderAdapter.provisionUser(any(), any(), any(), any()))
                .willReturn("iam-" + username);
        given(gatewayClient.verifyNik(any()))
                .willReturn(new VerifyNikResponse(
                        UUID.randomUUID().toString(),
                        request.nik(),
                        true,
                        request.fullName(),
                        "Jakarta",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Jl. Test",
                        "ACTIVE",
                        "00",
                        "Success"
                ));
        return userApplicationService.registerUser(request).get();
    }
}
