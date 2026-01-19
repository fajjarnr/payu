package id.payu.account.integration;

import id.payu.account.dto.RegisterUserRequest;
import id.payu.account.entity.User;
import id.payu.account.repository.UserRepository;
import id.payu.account.service.OnboardingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Integration Test using Testcontainers
 * Verifies full stack from Service to DB
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DisplayName("Onboarding Integration Test")
@org.junit.jupiter.api.Tag("integration")
class OnboardingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private id.payu.account.service.GatewayClient gatewayClient;

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }
    
    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @Test
    @DisplayName("should persist user to real database")
    void shouldPersistUserToDatabase() throws ExecutionException, InterruptedException {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                UUID.randomUUID().toString(),
                "integration-user",
                "integration@payu.id",
                "+628123456789",
                "Integration Test User",
                "3201234567890001"
        );

        given(gatewayClient.verifyNik(any()))
                .willReturn(new id.payu.account.dto.DukcapilResponse(
                        UUID.randomUUID().toString(),
                        request.nik(),
                        true,
                        "VALID",
                        "00",
                        "Success"
                ));

        // When
        User savedUser = onboardingService.registerUser(request).get();

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();

        // Verify direct DB persistence
        User userFromDb = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(userFromDb.getUsername()).isEqualTo("integration-user");
        assertThat(userFromDb.getEmail()).isEqualTo("integration@payu.id");
        assertThat(userFromDb.getKycStatus()).isEqualTo(User.KycStatus.APPROVED);
    }
    
    // Configure Testcontainers properties manually if @ServiceConnection fails or for specific overrides
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
         registry.add("spring.datasource.url", postgres::getJdbcUrl);
         registry.add("spring.datasource.username", postgres::getUsername);
         registry.add("spring.datasource.password", postgres::getPassword);
         registry.add("spring.flyway.enabled", () -> "true"); // Enable flyway for real DB
    }
}
