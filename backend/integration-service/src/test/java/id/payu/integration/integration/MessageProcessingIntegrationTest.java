package id.payu.integration.integration;

import id.payu.integration.config.TestSecurityConfig;
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
class MessageProcessingIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @Order(1)
    @DisplayName("Should return integration service info")
    void testGetIntegrationInfo() {
        given()
                .when()
                .get("/api/v1/integration")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("service", equalTo("integration-service"))
                .body("endpoints", hasItem("POST /api/v1/integration/swift/process"));
    }

    @Test
    @Order(2)
    @DisplayName("Should return integration service status")
    void testGetStatus() {
        given()
                .when()
                .get("/api/v1/integration/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("service", equalTo("integration-service"));
    }

    @Test
    @Order(3)
    @DisplayName("Should return 404 for non-existent message status")
    void testGetNonExistentMessageStatus() {
        given()
                .pathParam("messageId", "00000000-0000-0000-0000-000000000000")
                .when()
                .get("/api/v1/integration/messages/{messageId}/status")
                .then()
                .statusCode(404);
    }
}