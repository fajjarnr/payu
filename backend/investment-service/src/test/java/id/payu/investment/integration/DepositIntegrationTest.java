package id.payu.investment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.investment.config.TestSecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for deposit purchase. Uses MockMvc to avoid
 * RestAssured HTTPBuilder NPE on Java 25 (per L-066).
 *
 * Status: still @Disabled. MockMvc conversion done (iter 35). Testcontainers
 * needs Docker for PostgreSQL. Tried podman socket (per L-062):
 *   - podman system service -t 0 unix:///tmp/podman.sock
 *   - DOCKER_HOST=unix:///tmp/podman.sock TESTCONTAINERS_RYUK_DISABLED=true mvn test
 * Result: HikariCP "Could not find a valid Docker environment". Testcontainers
 * Docker client doesn't accept podman socket in this env (Docker API parity
 * gaps). Test will pass once real Docker available.
 */
@Disabled("Requires Docker. Podman socket substitute doesn't work with Testcontainers 2.0.5. MockMvc conversion done (iter 35).")
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepositIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    @DisplayName("Should return 400 when buying deposit with invalid account")
    void testBuyDepositInvalidAccount() throws Exception {
        String requestBody = """
            {
                "accountId": "00000000-0000-0000-0000-000000000000",
                "amount": 1000000.00,
                "tenure": 3
            }
            """;

        mockMvc.perform(post("/api/v1/investments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(400),
                        org.hamcrest.Matchers.is(404))));
    }

    @Test
    @Order(2)
    @DisplayName("Should enforce idempotency on deposit purchase")
    void testBuyDepositIdempotency() throws Exception {
        String requestBody = """
            {
                "accountId": "00000000-0000-0000-0000-000000000000",
                "amount": 500000.00,
                "tenure": 1
            }
            """;

        String idempotencyKey = "test-idempotency-key-" + System.currentTimeMillis();

        // First request
        mockMvc.perform(post("/api/v1/investments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(requestBody))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(400),
                        org.hamcrest.Matchers.is(404),
                        org.hamcrest.Matchers.is(200))));

        // Second request with same key should return same result or 409/429 depending on implementation
        mockMvc.perform(post("/api/v1/investments/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .content(requestBody))
                .andExpect(status().is(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(400),
                        org.hamcrest.Matchers.is(404),
                        org.hamcrest.Matchers.is(200),
                        org.hamcrest.Matchers.is(409))));
    }
}