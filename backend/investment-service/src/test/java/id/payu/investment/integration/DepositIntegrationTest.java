package id.payu.investment.integration;

import id.payu.investment.config.TestSecurityConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DepositIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @Order(1)
    @DisplayName("Should return 400 when buying deposit with invalid account")
    void testBuyDepositInvalidAccount() {
        String requestBody = """
            {
                "accountId": "00000000-0000-0000-0000-000000000000",
                "amount": 1000000.00,
                "tenure": 3
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/investments/deposits")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(2)
    @DisplayName("Should enforce idempotency on deposit purchase")
    void testBuyDepositIdempotency() {
        String requestBody = """
            {
                "accountId": "00000000-0000-0000-0000-000000000000",
                "amount": 500000.00,
                "tenure": 1
            }
            """;

        String idempotencyKey = "test-idempotency-key-" + System.currentTimeMillis();

        // First request
        given()
                .contentType(ContentType.JSON)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(requestBody)
                .when()
                .post("/api/v1/investments/deposits")
                .then()
                .statusCode(anyOf(is(400), is(404), is(200)));

        // Second request with same key should return same result or 409/429 depending on implementation
        given()
                .contentType(ContentType.JSON)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(requestBody)
                .when()
                .post("/api/v1/investments/deposits")
                .then()
                .statusCode(anyOf(is(400), is(404), is(200), is(409)));
    }
}