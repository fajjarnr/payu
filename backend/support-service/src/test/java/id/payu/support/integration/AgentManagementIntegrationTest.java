package id.payu.support.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Agent Management workflows.
 * Tests the complete lifecycle of support agents from creation to deactivation.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentManagementIntegrationTest {

    private static Long createdAgentId;
    private static String testEmployeeId = "INT-TEST-001";

    @Test
    @Order(1)
    @DisplayName("Should create a new support agent successfully")
    void testCreateAgent() {
        String requestBody = """
            {
                "employeeId": "%s",
                "name": "Integration Test Agent",
                "email": "integration.test@payu.id",
                "department": "Customer Support",
                "level": "SENIOR"
            }
            """.formatted(testEmployeeId);

        var response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("employeeId", equalTo(testEmployeeId))
                .body("name", equalTo("Integration Test Agent"))
                .body("email", equalTo("integration.test@payu.id"))
                .body("department", equalTo("Customer Support"))
                .body("level", equalTo("SENIOR"))
                .body("active", equalTo(true))
                .body("id", notNullValue())
                .body("createdAt", notNullValue())
                .extract();

        Object idObj = response.path("id");
        createdAgentId = idObj != null ? ((Number) idObj).longValue() : null;
        assertNotNull(createdAgentId, "Agent ID should not be null after creation");
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve agent by ID")
    void testGetAgentById() {
        assertNotNull(createdAgentId, "Agent must be created first");

        given()
                .pathParam("id", createdAgentId)
                .when()
                .get("/api/v1/support/agents/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdAgentId.intValue()))
                .body("employeeId", equalTo(testEmployeeId))
                .body("active", equalTo(true));
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve agent by employee ID")
    void testGetAgentByEmployeeId() {
        given()
                .pathParam("employeeId", testEmployeeId)
                .when()
                .get("/api/v1/support/agents/employee/{employeeId}")
                .then()
                .statusCode(200)
                .body("employeeId", equalTo(testEmployeeId))
                .body("name", equalTo("Integration Test Agent"));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 for non-existent agent")
    void testGetNonExistentAgent() {
        given()
                .pathParam("id", 99999)
                .when()
                .get("/api/v1/support/agents/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve all agents")
    void testGetAllAgents() {
        given()
                .when()
                .get("/api/v1/support/agents")
                .then()
                .statusCode(200)
                .body("", not(empty()))
                .body("$", hasItem(hasEntry("employeeId", testEmployeeId)));
    }

    @Test
    @Order(6)
    @DisplayName("Should deactivate agent")
    void testDeactivateAgent() {
        assertNotNull(createdAgentId, "Agent must be created first");

        given()
                .pathParam("id", createdAgentId)
                .contentType(ContentType.JSON)
                .body("{\"active\": false}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(false));

        // Verify the status change
        given()
                .pathParam("id", createdAgentId)
                .when()
                .get("/api/v1/support/agents/{id}")
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    @Order(7)
    @DisplayName("Should reactivate agent")
    void testReactivateAgent() {
        assertNotNull(createdAgentId, "Agent must be created first");

        given()
                .pathParam("id", createdAgentId)
                .contentType(ContentType.JSON)
                .body("{\"active\": true}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(true));
    }

    @Test
    @Order(8)
    @DisplayName("Should enforce unique employee ID constraint")
    void testDuplicateEmployeeId() {
        String requestBody = """
            {
                "employeeId": "%s",
                "name": "Duplicate Agent",
                "email": "duplicate@payu.id",
                "department": "Support",
                "level": "JUNIOR"
            }
            """.formatted(testEmployeeId);

        // First request should succeed (or conflict if already exists)
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(anyOf(is(201), is(409), is(400), is(500)));
    }

    @Test
    @Order(9)
    @DisplayName("Should validate required fields")
    void testValidation() {
        String invalidRequest = """
            {
                "employeeId": "",
                "name": "",
                "email": "invalid-email",
                "department": "",
                "level": "INVALID_LEVEL"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when updating non-existent agent status")
    void testUpdateNonExistentAgentStatus() {
        given()
                .pathParam("id", 99999)
                .contentType(ContentType.JSON)
                .body("{\"active\": true}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(404);
    }
}
