package id.payu.support.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for notification triggers in support workflows.
 * Tests events that should trigger notifications to agents and administrators.
 *
 * Note: This service does not directly send notifications but logs events
 * that would trigger notifications in a production environment.
 * NotificationService integration would be tested in a separate test suite.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationTriggersIntegrationTest {

    private static Long agentId;
    private static Long moduleId;
    private static Long trainingId;

    private Long extractId(Object obj) {
        return obj != null ? ((Number) obj).longValue() : null;
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Create test data for notification scenarios")
    void setupTestData() {
        // Create agent
        String agentRequest = """
            {
                "employeeId": "NOTIF-001",
                "name": "Notification Test Agent",
                "email": "notif.agent@payu.id",
                "department": "Customer Support",
                "level": "SENIOR"
            }
            """;

        Object createdAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        agentId = extractId(createdAgentIdObj);

        // Create training module
        String moduleRequest = """
            {
                "code": "NOTIF-MOD-001",
                "title": "Notification Test Module",
                "description": "Module for testing notification triggers",
                "category": "COMPLIANCE",
                "durationMinutes": 60,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Object createdModuleIdObj = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        moduleId = extractId(createdModuleIdObj);
    }

    @Test
    @Order(2)
    @DisplayName("NOTIF-001: Should trigger notification when training is assigned")
    void testTrainingAssignedNotification() {
        // When training is assigned to an agent, a notification should be triggered
        // In production, this would send email/push notification to the agent
        String assignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "NOT_STARTED",
                "notes": "Training assigned - notification expected"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("agentId", equalTo(agentId.intValue()))
                .body("trainingModuleId", equalTo(moduleId.intValue()))
                .body("status", equalTo("NOT_STARTED"));

        // Verify the training exists and is retrievable
        given()
                .pathParam("agentId", agentId)
                .pathParam("moduleId", moduleId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/module/{moduleId}")
                .then()
                .statusCode(200)
                .body("notes", equalTo("Training assigned - notification expected"));
    }

    @Test
    @Order(3)
    @DisplayName("NOTIF-002: Should trigger notification when training is started")
    void testTrainingStartedNotification() {
        // When agent starts training, notify team lead
        String startRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Training started - team lead notification expected"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(startRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"))
                .body("startedAt", notNullValue());

        // Verify the status change persisted
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}")
                .then()
                .statusCode(200)
                .body("$", hasItem(hasEntry("status", "IN_PROGRESS")));
    }

    @Test
    @Order(4)
    @DisplayName("NOTIF-003: Should trigger notification when training is completed successfully")
    void testTrainingCompletedNotification() {
        // When training is passed, trigger success notification
        String completeRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 95,
                "notes": "Training passed - success notification expected"
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

        // Agent should now be fully trained (only 1 mandatory module in this test)
        given()
                .pathParam("agentId", agentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("$", hasKey("fullyTrained"));
    }

    @Test
    @Order(5)
    @DisplayName("NOTIF-004: Should trigger notification when training is failed")
    void testTrainingFailedNotification() {
        // Create another agent for failure scenario
        String agentRequest = """
            {
                "employeeId": "NOTIF-FAIL-001",
                "name": "Failure Test Agent",
                "email": "fail.agent@payu.id",
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

        Long failAgentId = extractId(failAgentIdObj);

        // Create another module
        String moduleRequest = """
            {
                "code": "NOTIF-FAIL-MOD",
                "title": "Failure Test Module",
                "description": "Module for testing failure notifications",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 30,
                "status": "ACTIVE",
                "mandatory": true
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

        Long failModuleId = extractId(failModuleIdObj);

        // Fail the training - should trigger alert to team lead
        String failRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "FAILED",
                "score": 45,
                "notes": "Training failed - alert notification expected"
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
                .body("completedAt", notNullValue());

        // Agent should NOT be fully trained
        given()
                .pathParam("agentId", failAgentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("fullyTrained", equalTo(false));
    }

    @Test
    @Order(6)
    @DisplayName("NOTIF-005: Should trigger notification when mandatory module is created")
    void testMandatoryModuleCreatedNotification() {
        // When a mandatory module is created, notify all agents
        String mandatoryModuleRequest = """
            {
                "code": "NOTIF-MAND-NEW",
                "title": "New Mandatory Training",
                "description": "New mandatory training - broadcast notification expected",
                "category": "COMPLIANCE",
                "durationMinutes": 90,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(mandatoryModuleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("mandatory", equalTo(true))
                .body("status", equalTo("ACTIVE"));

        // Verify it appears in mandatory modules list
        given()
                .when()
                .get("/api/v1/support/modules/mandatory")
                .then()
                .statusCode(200)
                .body("$", hasItem(hasEntry("code", "NOTIF-MAND-NEW")));
    }

    @Test
    @Order(7)
    @DisplayName("NOTIF-006: Should trigger notification when agent is created")
    void testAgentCreatedNotification() {
        // When a new agent is created, notify team leads and managers
        String newAgentRequest = """
            {
                "employeeId": "NOTIF-NEW-001",
                "name": "New Hire Agent",
                "email": "newhire@payu.id",
                "department": "Customer Support",
                "level": "JUNIOR"
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(newAgentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("employeeId", equalTo("NOTIF-NEW-001"))
                .body("level", equalTo("JUNIOR"));

        // Verify agent is in the system
        given()
                .pathParam("employeeId", "NOTIF-NEW-001")
                .when()
                .get("/api/v1/support/agents/employee/{employeeId}")
                .then()
                .statusCode(200)
                .body("name", equalTo("New Hire Agent"));
    }

    @Test
    @Order(8)
    @DisplayName("NOTIF-007: Should trigger notification when agent status changes")
    void testAgentStatusChangeNotification() {
        // When agent is deactivated, notify administrators
        String statusChangeRequest = """
            {
                "employeeId": "NOTIF-STATUS-001",
                "name": "Status Change Agent",
                "email": "status.change@payu.id",
                "department": "Support",
                "level": "SENIOR"
            }
            """;

        Object statusAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(statusChangeRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long statusAgentId = extractId(statusAgentIdObj);

        // Deactivate - should trigger notification
        given()
                .pathParam("id", statusAgentId)
                .contentType(ContentType.JSON)
                .body("{\"active\": false}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(false));

        // Reactivate - should trigger notification
        given()
                .pathParam("id", statusAgentId)
                .contentType(ContentType.JSON)
                .body("{\"active\": true}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(true));
    }

    @Test
    @Order(9)
    @DisplayName("NOTIF-008: Should trigger notification when module status changes")
    void testModuleStatusChangeNotification() {
        // Create a module
        String moduleRequest = """
            {
                "code": "NOTIF-STATUS-MOD",
                "title": "Status Change Module",
                "description": "Module for testing status change notifications",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 45,
                "status": "DRAFT",
                "mandatory": false
            }
            """;

        Object modIdObj = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long modId = extractId(modIdObj);

        // Activate - should notify all agents
        given()
                .pathParam("id", modId)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ACTIVE\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Archive - should notify administrators
        given()
                .pathParam("id", modId)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ARCHIVED\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ARCHIVED"));
    }

    @Test
    @Order(10)
    @DisplayName("NOTIF-009: Should trigger notification for training deadline approaching")
    void testTrainingDeadlineNotification() {
        // This would trigger a notification for agents with pending training
        // In production, this would be a scheduled job checking for incomplete trainings
        // Here we verify that the API can identify agents needing reminders

        // Create agent with incomplete training
        String agentRequest = """
            {
                "employeeId": "NOTIF-PENDING-001",
                "name": "Pending Training Agent",
                "email": "pending@payu.id",
                "department": "Support",
                "level": "JUNIOR"
            }
            """;

        Object pendingAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long pendingAgentId = extractId(pendingAgentIdObj);

        // Create mandatory module
        String moduleRequest = """
            {
                "code": "NOTIF-PEND-MOD",
                "title": "Pending Training Module",
                "description": "Module for testing deadline notifications",
                "category": "COMPLIANCE",
                "durationMinutes": 60,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Object pendingModIdObj = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long pendingModId = extractId(pendingModIdObj);

        // Assign but don't complete
        String assignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "NOT_STARTED",
                "notes": "Pending completion - reminder notification expected"
            }
            """.formatted(pendingAgentId, pendingModId);

        given()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("NOT_STARTED"));

        // Verify agent is not fully trained (would trigger reminder)
        given()
                .pathParam("agentId", pendingAgentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("fullyTrained", equalTo(false));
    }

    @Test
    @Order(11)
    @DisplayName("NOTIF-010: Should trigger notification for training reassignment after failure")
    void testRetrainingNotification() {
        // Create agent who failed and needs retraining
        String agentRequest = """
            {
                "employeeId": "NOTIF-RETRY-001",
                "name": "Retry Training Agent",
                "email": "retry.agent@payu.id",
                "department": "Support",
                "level": "JUNIOR"
            }
            """;

        Object retryAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long retryAgentId = extractId(retryAgentIdObj);

        // Create module
        String moduleRequest = """
            {
                "code": "NOTIF-RETRY-MOD",
                "title": "Retry Training Module",
                "description": "Module for testing retraining notifications",
                "category": "COMPLIANCE",
                "durationMinutes": 45,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Object retryModIdObj = given()
                .contentType(ContentType.JSON)
                .body(moduleRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long retryModId = extractId(retryModIdObj);

        // First attempt - fail
        String failRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "FAILED",
                "score": 40,
                "notes": "Failed - retraining notification expected"
            }
            """.formatted(retryAgentId, retryModId);

        given()
                .contentType(ContentType.JSON)
                .body(failRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"));

        // Reassign for retry - should trigger retraining notification
        String reassignRequest = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Retraining assigned - notification expected"
            }
            """.formatted(retryAgentId, retryModId);

        given()
                .contentType(ContentType.JSON)
                .body(reassignRequest)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @Order(12)
    @DisplayName("NOTIF-011: Verify overall training status notification trigger")
    void testOverallStatusNotification() {
        // Check overall status - would trigger notification if training percentage is low
        var response = given()
                .when()
                .get("/api/v1/support/training-status")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("");

        assertNotNull(response.get("activeAgents"));
        assertNotNull(response.get("trainedAgents"));
        assertNotNull(response.get("trainingPercentage"));

        // In production, if trainingPercentage < threshold, trigger admin notification
        Double trainingPercentage = ((Number) response.get("trainingPercentage")).doubleValue();
        assertTrue(trainingPercentage >= 0.0, "Training percentage should be valid");
    }
}
