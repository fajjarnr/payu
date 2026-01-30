package id.payu.support.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Training Module management workflows.
 * Tests the complete lifecycle of training modules from creation to archival.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrainingModuleIntegrationTest {

    private static Long moduleId;
    private static Long mandatoryModuleId;
    private static String testModuleCode = "INT-TM-001";

    @Test
    @Order(1)
    @DisplayName("Should create a training module successfully")
    void testCreateTrainingModule() {
        String requestBody = """
            {
                "code": "%s",
                "title": "Integration Test Training Module",
                "description": "A comprehensive training module for integration testing",
                "category": "ONBOARDING",
                "durationMinutes": 120,
                "status": "DRAFT",
                "mandatory": false
            }
            """.formatted(testModuleCode);

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("code", equalTo(testModuleCode))
                .body("title", equalTo("Integration Test Training Module"))
                .body("category", equalTo("ONBOARDING"))
                .body("durationMinutes", equalTo(120))
                .body("status", equalTo("DRAFT"))
                .body("mandatory", equalTo(false))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .extract();

        Object idObj = response.path("id");
        moduleId = idObj != null ? ((Number) idObj).longValue() : null;
        assertNotNull(moduleId, "Module ID should not be null after creation");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a mandatory training module")
    void testCreateMandatoryTrainingModule() {
        String requestBody = """
            {
                "code": "MANDATORY-001",
                "title": "Mandatory Compliance Training",
                "description": "Required compliance training for all agents",
                "category": "COMPLIANCE",
                "durationMinutes": 60,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("mandatory", equalTo(true))
                .body("status", equalTo("ACTIVE"))
                .extract();

        Object idObj = response.path("id");
        mandatoryModuleId = idObj != null ? ((Number) idObj).longValue() : null;
        assertNotNull(mandatoryModuleId, "Mandatory module ID should not be null");
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve module by ID")
    void testGetModuleById() {
        assertNotNull(moduleId, "Module must be created first");

        given()
                .pathParam("id", moduleId)
                .when()
                .get("/api/v1/support/modules/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(moduleId.intValue()))
                .body("code", equalTo(testModuleCode))
                .body("title", equalTo("Integration Test Training Module"));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 for non-existent module")
    void testGetNonExistentModule() {
        given()
                .pathParam("id", 99999)
                .when()
                .get("/api/v1/support/modules/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve all training modules")
    void testGetAllModules() {
        given()
                .when()
                .get("/api/v1/support/modules")
                .then()
                .statusCode(200)
                .body("", not(empty()))
                .body("$", hasItem(hasEntry("code", testModuleCode)));
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve mandatory training modules only")
    void testGetMandatoryModules() {
        given()
                .when()
                .get("/api/v1/support/modules/mandatory")
                .then()
                .statusCode(200)
                .body("", not(empty()))
                .body("mandatory", everyItem(equalTo(true)))
                .body("$", hasItem(hasEntry("code", "MANDATORY-001")));
    }

    @Test
    @Order(7)
    @DisplayName("Should update module status to ACTIVE")
    void testActivateModule() {
        assertNotNull(moduleId, "Module must be created first");

        given()
                .pathParam("id", moduleId)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ACTIVE\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Verify the status change
        given()
                .pathParam("id", moduleId)
                .when()
                .get("/api/v1/support/modules/{id}")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @Order(8)
    @DisplayName("Should update module status to ARCHIVED")
    void testArchiveModule() {
        // First create a module to archive
        String requestBody = """
            {
                "code": "TO-ARCHIVE",
                "title": "Module to Archive",
                "description": "This module will be archived",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 30,
                "status": "ACTIVE",
                "mandatory": false
            }
            """;

        Object archiveIdObj = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long archiveModuleId = archiveIdObj != null ? ((Number) archiveIdObj).longValue() : null;
        assertNotNull(archiveModuleId, "Archive module ID should not be null");

        // Archive it
        given()
                .pathParam("id", archiveModuleId)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ARCHIVED\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ARCHIVED"));
    }

    @Test
    @Order(9)
    @DisplayName("Should create modules for all categories")
    void testCreateAllCategoryModules() {
        String[] categories = {
            "ONBOARDING", "PRODUCT_KNOWLEDGE", "COMPLIANCE",
            "SYSTEMS", "COMMUNICATION", "DISPUTE_RESOLUTION", "SECURITY"
        };

        for (String category : categories) {
            String requestBody = """
                {
                    "code": "CAT-%s-TEST",
                    "title": "%s Training",
                    "description": "Test module for %s category",
                    "category": "%s",
                    "durationMinutes": 45,
                    "status": "ACTIVE",
                    "mandatory": false
                }
                """.formatted(category, category, category.toLowerCase(), category);

            given()
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/support/modules")
                    .then()
                    .statusCode(201)
                    .body("category", equalTo(category));
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when updating non-existent module status")
    void testUpdateNonExistentModuleStatus() {
        given()
                .pathParam("id", 99999)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ACTIVE\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @DisplayName("Should validate required fields for module creation")
    void testModuleValidation() {
        String invalidRequest = """
            {
                "code": "",
                "title": "",
                "category": "INVALID_CATEGORY",
                "durationMinutes": -10,
                "status": "INVALID_STATUS"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }
}
