package id.payu.support.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Agent Training workflows.
 * Tests training assignment, status transitions, and completion tracking.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentTrainingWorkflowIntegrationTest {

    private static Long agentId;
    private static Long moduleId;
    private static Long mandatoryModuleId;
    private static Long trainingId;
    private static String testEmployeeId = "TRN-TEST-001";

    @Test
    @Order(1)
    @DisplayName("Setup: Create test agent and training modules")
    void setupTestData() {
        // Create agent
        String agentRequest = """
            {
                "employeeId": "%s",
                "name": "Training Test Agent",
                "email": "training.test@payu.id",
                "department": "Customer Support",
                "level": "JUNIOR"
            }
            """.formatted(testEmployeeId);

        Integer createdAgentId = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        assertNotNull(createdAgentId, "Agent should be created");
        agentId = createdAgentId.longValue();

        // Create regular training module
        String moduleRequest = """
            {
                "code": "TRN-MOD-001",
                "title": "Customer Service Essentials",
                "description": "Essential skills for customer service",
                "category": "COMMUNICATION",
                "durationMinutes": 90,
                "status": "ACTIVE",
                "mandatory": false
            }
            """;

        Integer createdModuleId = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        assertNotNull(createdModuleId, "Module should be created");
        moduleId = createdModuleId.longValue();

        // Create mandatory training module
        String mandatoryModuleRequest = """
            {
                "code": "TRN-MAND-001",
                "title": "Mandatory Safety Training",
                "description": "Required safety training for all agents",
                "category": "COMPLIANCE",
                "durationMinutes": 60,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Integer createdMandatoryId = given()
                .contentType(ContentType.JSON)
                .body(mandatoryModuleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        assertNotNull(createdMandatoryId, "Mandatory module should be created");
        mandatoryModuleId = createdMandatoryId.longValue();
    }

    @Test
    @Order(2)
    @DisplayName("Should assign training to agent with NOT_STARTED status")
    void testAssignTraining() {
        String assignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "NOT_STARTED",
                "notes": "Assigned for completion"
            }
            """.formatted(agentId, moduleId);

        Object trainingIdObj = given()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("agentId", equalTo(agentId.intValue()))
                .body("trainingModuleId", equalTo(moduleId.intValue()))
                .body("status", equalTo("NOT_STARTED"))
                .body("agentName", equalTo("Training Test Agent"))
                .body("trainingModuleName", equalTo("Customer Service Essentials"))
                .body("id", notNullValue())
                .extract()
                .path("id");

        assertNotNull(trainingIdObj, "Training ID should not be null");
        trainingId = ((Number) trainingIdObj).longValue();
    }

    @Test
    @Order(3)
    @DisplayName("Should update training status to IN_PROGRESS")
    void testStartTraining() {
        String updateRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Started training"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"))
                .body("startedAt", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve trainings by agent")
    void testGetTrainingsByAgent() {
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}")
                .then()
                .statusCode(200)
                .body("", not(empty()))
                .body("$", hasItem(hasEntry("agentId", agentId.intValue())));
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve trainings by module")
    void testGetTrainingsByModule() {
        given()
                .pathParam("moduleId", moduleId)
                .when()
                .get("/api/v1/support/trainings/module/{moduleId}")
                .then()
                .statusCode(200)
                .body("", not(empty()))
                .body("$", hasItem(hasEntry("trainingModuleId", moduleId.intValue())));
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve specific agent training")
    void testGetAgentTraining() {
        given()
                .pathParam("agentId", agentId)
                .pathParam("moduleId", moduleId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/module/{moduleId}")
                .then()
                .statusCode(200)
                .body("agentId", equalTo(agentId.intValue()))
                .body("trainingModuleId", equalTo(moduleId.intValue()))
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @Order(7)
    @DisplayName("Should complete training with PASSED status")
    void testCompleteTrainingPassed() {
        String completeRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 95,
                "notes": "Excellent performance"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(completeRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("PASSED"))
                .body("score", equalTo(95))
                .body("completedAt", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Should assign mandatory training and check completion status")
    void testMandatoryTrainingAssignment() {
        // Assign mandatory training
        String assignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Mandatory training assigned"
            }
            """.formatted(agentId, mandatoryModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"));

        // Check if agent is fully trained (should be false since mandatory not complete)
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("agentId", equalTo(agentId.intValue()))
                .body("fullyTrained", equalTo(false));

        // Complete mandatory training
        String completeRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 100,
                "notes": "Mandatory training completed"
            }
            """.formatted(agentId, mandatoryModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(completeRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("PASSED"));

        // Now agent should be fully trained
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("agentId", equalTo(agentId.intValue()))
                .body("fullyTrained", equalTo(true));
    }

    @Test
    @Order(9)
    @DisplayName("Should handle training failure status")
    void testTrainingFailure() {
        // Create another agent and module for failure test
        String agentRequest = """
            {
                "employeeId": "FAIL-TEST-001",
                "name": "Failure Test Agent",
                "email": "failure.test@payu.id",
                "department": "Support",
                "level": "JUNIOR"
            }
            """;

        Object failAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long failAgentId = failAgentIdObj != null ? ((Number) failAgentIdObj).longValue() : null;

        String moduleRequest = """
            {
                "code": "FAIL-MOD-001",
                "title": "Failure Test Module",
                "description": "Module for testing failure scenario",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 30,
                "status": "ACTIVE",
                "mandatory": false
            }
            """;

        Object failModuleIdObj = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long failModuleId = failModuleIdObj != null ? ((Number) failModuleIdObj).longValue() : null;

        String failRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "FAILED",
                "score": 45,
                "notes": "Did not pass minimum requirements"
            }
            """.formatted(failAgentId, failModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(failRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"))
                .body("score", equalTo(45))
                .body("completedAt", notNullValue())
                .body("notes", equalTo("Did not pass minimum requirements"));
    }

    @Test
    @Order(10)
    @DisplayName("Should retrieve all trainings")
    void testGetAllTrainings() {
        given()
                .when()
                .get("/api/v1/support/trainings")
                .then()
                .statusCode(200)
                .body("", not(empty()));
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 for non-existent agent training")
    void testGetNonExistentAgentTraining() {
        given()
                .pathParam("agentId", 99999)
                .pathParam("moduleId", 99999)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/module/{moduleId}")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(12)
    @DisplayName("Should prevent duplicate training assignments (idempotent update)")
    void testIdempotentTrainingAssignment() {
        // Assign same training again - should update existing
        String assignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 98,
                "notes": "Updated score"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("score", equalTo(98));
    }

    @Test
    @Order(13)
    @DisplayName("Should validate training assignment constraints")
    void testTrainingAssignmentValidation() {
        // Assign to non-existent agent
        String invalidRequest = """
            {
                "agentId": 99999,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS"
            }
            """.formatted(moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(400), is(404), is(500)));

        // Assign non-existent module
        String invalidModuleRequest = """
            {
                "agentId": %d,
                "trainingModuleId": 99999,
                "status": "IN_PROGRESS"
            }
            """.formatted(agentId);

        given()
                .contentType(ContentType.JSON)
                .body(invalidModuleRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(400), is(404), is(500)));
    }

    @Test
    @Order(14)
    @DisplayName("Should check training status endpoint")
    void testTrainingStatusEndpoint() {
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("agentId", equalTo(agentId.intValue()))
                .body("$", hasKey("fullyTrained"));
    }
}
