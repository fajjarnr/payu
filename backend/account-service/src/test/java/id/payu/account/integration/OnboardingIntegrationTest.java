package id.payu.account.integration;

import id.payu.account.adapter.client.GatewayClient;
import id.payu.account.adapter.client.IdentityProviderAdapter;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.application.service.UserApplicationService;
import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.User;
import id.payu.account.interfaces.dto.RegisterUserRequest;
import id.payu.account.interfaces.dto.VerifyNikResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Onboarding Integration Test")
@org.junit.jupiter.api.Tag("integration")
class OnboardingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserApplicationService userApplicationService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GatewayClient gatewayClient;

    @MockitoBean
    private IdentityProviderAdapter identityProviderAdapter;

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
                "integration@payu.fajjjar.my.id",
                "+628123456789",
                "Integration Test UserEntity",
                "3201234567890001",
                "SecureP@ss123"
        );

        given(gatewayClient.verifyNik(any()))
                .willReturn(new VerifyNikResponse(
                        UUID.randomUUID().toString(),
                        request.nik(),
                        true,
                        "Integration Test UserEntity",
                        "Jakarta",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "Jl. Test",
                        "ACTIVE",
                        "00",
                        "Success"
                ));
        given(identityProviderAdapter.provisionUser(any(), any(), any(), any()))
                .willReturn("iam-" + request.username());

        // When
        User savedUser = userApplicationService.registerUser(request).get();

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();

        // Verify direct DB persistence
        id.payu.account.adapter.persistence.entity.UserEntity userFromDb = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(userFromDb.getUsername()).isEqualTo("integration-user");
        assertThat(userFromDb.getEmail()).isEqualTo("integration@payu.fajjjar.my.id");
        assertThat(userFromDb.getKycStatus().name()).isEqualTo(KycStatus.APPROVED.name());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
         // INTEGRATION-CTX: the app binds datasource-starter prefix
         // spring.datasource.primary.hikari.* — plain spring.datasource.url
         // is ignored, which previously left the context without an EMF.
         registry.add("spring.datasource.primary.hikari.jdbc-url", postgres::getJdbcUrl);
         registry.add("spring.datasource.primary.hikari.username", postgres::getUsername);
         registry.add("spring.datasource.primary.hikari.password", postgres::getPassword);
         registry.add("spring.datasource.primary.hikari.driver-class-name", () -> "org.postgresql.Driver");
         registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
         registry.add("spring.flyway.enabled", () -> "true");
    }
}
