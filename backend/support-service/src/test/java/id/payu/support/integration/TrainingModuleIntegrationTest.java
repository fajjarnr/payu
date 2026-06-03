package id.payu.support.integration;

import id.payu.support.config.TestSecurityConfig;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrainingModuleIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private static Long createdModuleId;

    @Test
    @Order(1)
    @DisplayName("Should create a new training module successfully")
    void testCreateTrainingModule() {
        String requestBody = """
            {
                "title": "Integration Test Module",
                "description": "Module for integration testing",
                "durationMinutes": 60,
                "isMandatory": true
            }
            """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("data.title", equalTo("Integration Test Module"))
                .body("data.description", equalTo("Module for integration testing"))
                .body("data.durationMinutes", equalTo(60))
                .body("data.mandatory", equalTo(true))
                .body("data.status", equalTo("DRAFT"))
                .body("data.id", notNullValue())
                .extract();

        Object idObj = response.path("data.id");
        createdModuleId = idObj != null ? ((Number) idObj).longValue() : null;
        assertNotNull(createdModuleId, "Module ID should not be null after creation");
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve module by ID")
    void testGetModuleById() {
        assertNotNull(createdModuleId, "Module must be created first");

        given()
                .pathParam("id", createdModuleId)
                .when()
                .get("/api/v1/support/modules/{id}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(createdModuleId.intValue()))
                .body("data.title", equalTo("Integration Test Module"));
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve all mandatory modules")
    void testGetMandatoryModules() {
        given()
                .when()
                .get("/api/v1/support/modules/mandatory")
                .then()
                .statusCode(200)
                .body("data", not(empty()))
                .body("data", hasItem(hasEntry("title", "Integration Test Module")));
    }

    @Test
    @Order(4)
    @DisplayName("Should update module status to ACTIVE")
    void testUpdateModuleStatus() {
        assertNotNull(createdModuleId, "Module must be created first");

        given()
                .pathParam("id", createdModuleId)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "ACTIVE"))
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("data.status", equalTo("ACTIVE"));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent module")
    void testGetNonExistentModule() {
        given()
                .pathParam("id", 99999)
                .when()
                .get("/api/v1/support/modules/{id}")
                .then()
                .statusCode(404);
    }
}