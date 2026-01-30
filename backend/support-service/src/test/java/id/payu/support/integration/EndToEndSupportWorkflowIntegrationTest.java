package id.payu.support.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End integration tests for complete Support workflows.
 * Tests realistic scenarios involving agents, training modules, and assignments.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndSupportWorkflowIntegrationTest {

    private static Long teamLeadId;
    private static Long seniorAgentId;
    private static Long juniorAgentId;
    private static Long onboardingModuleId;
    private static Long complianceModuleId;
    private static Long productKnowledgeModuleId;

    private Long extractId(Object obj) {
        return obj != null ? ((Number) obj).longValue() : null;
    }

    @Test
    @Order(1)
    @DisplayName("E2E Scenario 1: Setup support team structure")
    void testSetupSupportTeam() {
        // Create Team Lead
        String teamLeadRequest = """
            {
                "employeeId": "E2E-TL-001",
                "name": "Sarah Team Lead",
                "email": "sarah.tl@payu.id",
                "department": "Customer Support",
                "level": "TEAM_LEAD"
            }
            """;

        Object tlIdObj = given()
                .contentType(ContentType.JSON)
                .body(teamLeadRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("level", equalTo("TEAM_LEAD"))
                .extract()
                .path("id");

        teamLeadId = extractId(tlIdObj);

        // Create Senior Agent
        String seniorRequest = """
            {
                "employeeId": "E2E-SE-001",
                "name": "Mike Senior",
                "email": "mike.senior@payu.id",
                "department": "Customer Support",
                "level": "SENIOR"
            }
            """;

        Object seIdObj = given()
                .contentType(ContentType.JSON)
                .body(seniorRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("level", equalTo("SENIOR"))
                .extract()
                .path("id");

        seniorAgentId = extractId(seIdObj);

        // Create Junior Agent
        String juniorRequest = """
            {
                "employeeId": "E2E-JR-001",
                "name": "Alex Junior",
                "email": "alex.jr@payu.id",
                "department": "Customer Support",
                "level": "JUNIOR"
            }
            """;

        Object jrIdObj = given()
                .contentType(ContentType.JSON)
                .body(juniorRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("level", equalTo("JUNIOR"))
                .extract()
                .path("id");

        juniorAgentId = extractId(jrIdObj);
    }

    @Test
    @Order(2)
    @DisplayName("E2E Scenario 2: Create comprehensive training curriculum")
    void testCreateTrainingCurriculum() {
        // Onboarding module (mandatory)
        String onboardingRequest = """
            {
                "code": "E2E-ONBOARD",
                "title": "New Agent Onboarding",
                "description": "Complete onboarding for new support agents",
                "category": "ONBOARDING",
                "durationMinutes": 240,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Object obIdObj = given()
                .contentType(ContentType.JSON)
                .body(onboardingRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("mandatory", equalTo(true))
                .extract()
                .path("id");

        onboardingModuleId = extractId(obIdObj);

        // Compliance module (mandatory)
        String complianceRequest = """
            {
                "code": "E2E-COMPLY",
                "title": "Regulatory Compliance",
                "description": "Financial regulations and compliance training",
                "category": "COMPLIANCE",
                "durationMinutes": 180,
                "status": "ACTIVE",
                "mandatory": true
            }
            """;

        Object cmpIdObj = given()
                .contentType(ContentType.JSON)
                .body(complianceRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("mandatory", equalTo(true))
                .extract()
                .path("id");

        complianceModuleId = extractId(cmpIdObj);

        // Product Knowledge module (optional)
        String productRequest = """
            {
                "code": "E2E-PROD",
                "title": "Product Knowledge",
                "description": "In-depth product features and services",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 120,
                "status": "ACTIVE",
                "mandatory": false
            }
            """;

        Object prodIdObj = given()
                .contentType(ContentType.JSON)
                .body(productRequest)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("mandatory", equalTo(false))
                .extract()
                .path("id");

        productKnowledgeModuleId = extractId(prodIdObj);
    }

    @Test
    @Order(3)
    @DisplayName("E2E Scenario 3: New agent onboarding workflow")
    void testNewAgentOnboardingWorkflow() {
        // Junior agent starts onboarding
        String assignOnboarding = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Started onboarding process"
            }
            """.formatted(juniorAgentId, onboardingModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(assignOnboarding)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("IN_PROGRESS"))
                .body("startedAt", notNullValue());

        // Check training status - should not be fully trained yet
        given()
                .pathParam("agentId", juniorAgentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("fullyTrained", equalTo(false));
    }

    @Test
    @Order(4)
    @DisplayName("E2E Scenario 4: Complete onboarding and assign compliance")
    void testCompleteOnboardingAndAssignCompliance() {
        // Complete onboarding
        String completeOnboarding = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 88,
                "notes": "Successfully completed onboarding"
            }
            """.formatted(juniorAgentId, onboardingModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(completeOnboarding)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("PASSED"))
                .body("completedAt", notNullValue());

        // Assign compliance training
        String assignCompliance = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Started compliance training"
            }
            """.formatted(juniorAgentId, complianceModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(assignCompliance)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @Order(5)
    @DisplayName("E2E Scenario 5: Senior agent demonstrates full training completion")
    void testSeniorAgentFullCompletion() {
        // Get all mandatory modules (there may be more than just the 2 created in this test)
        var mandatoryModules = given()
                .when()
                .get("/api/v1/support/modules/mandatory")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        // Complete all mandatory modules for senior agent
        for (var module : mandatoryModules) {
            Map<?, ?> mod = (Map<?, ?>) module;
            Long modId = ((Number) mod.get("id")).longValue();
            String modTitle = (String) mod.get("title");

            String assignRequest = """
                {
                    "agentId": %d,
                    "trainingModuleId": %d,
                    "status": "PASSED",
                    "score": 92,
                    "notes": "Completed %s"
                }
                """.formatted(seniorAgentId, modId, modTitle);

            given()
                    .contentType(ContentType.JSON)
                    .body(assignRequest)
                    .when()
                    .post("/api/v1/support/trainings/assign")
                    .then()
                    .statusCode(anyOf(is(201), is(200)));
        }

        // Senior agent should now be fully trained
        given()
                .pathParam("agentId", seniorAgentId)
                .when()
                .get("/api/v1/support/trainings/agent/{agentId}/status")
                .then()
                .statusCode(200)
                .body("fullyTrained", equalTo(true));
    }

    @Test
    @Order(6)
    @DisplayName("E2E Scenario 6: Check overall training status")
    void testOverallTrainingStatus() {
        given()
                .when()
                .get("/api/v1/support/training-status")
                .then()
                .statusCode(200)
                .body("$", hasKey("activeAgents"))
                .body("$", hasKey("trainedAgents"))
                .body("$", hasKey("trainingPercentage"))
                .body("activeAgents", greaterThanOrEqualTo(0))
                .body("trainedAgents", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(7)
    @DisplayName("E2E Scenario 7: Team Lead tracks team progress")
    void testTeamProgressTracking() {
        // Get all trainings to see team progress
        var allTrainings = given()
                .when()
                .get("/api/v1/support/trainings")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        assertNotNull(allTrainings, "Trainings list should not be null");
        assertTrue(allTrainings.size() >= 3, "Should have at least 3 training assignments");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Scenario 8: Agent requires retraining after failure")
    void testRetrainingScenario() {
        // Create a new agent who fails initial training
        String agentRequest = """
            {
                "employeeId": "E2E-RETRY-001",
                "name": "Retry Agent",
                "email": "retry@payu.id",
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

        // First attempt - fail
        String failAttempt = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "FAILED",
                "score": 55,
                "notes": "Failed first attempt"
            }
            """.formatted(retryAgentId, onboardingModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(failAttempt)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"));

        // Second attempt - start again
        String retryAttempt = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Retrying after additional study"
            }
            """.formatted(retryAgentId, onboardingModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(retryAttempt)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"));

        // Final attempt - pass
        String passAttempt = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "PASSED",
                "score": 78,
                "notes": "Passed on retry"
            }
            """.formatted(retryAgentId, onboardingModuleId);

        given()
                .contentType(ContentType.JSON)
                .body(passAttempt)
                .when()
                .post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("PASSED"))
                .body("score", equalTo(78));
    }

    @Test
    @Order(9)
    @DisplayName("E2E Scenario 9: Module lifecycle management")
    void testModuleLifecycle() {
        // Create a new module in DRAFT
        String draftModule = """
            {
                "code": "E2E-DRAFT-001",
                "title": "Draft Module",
                "description": "Module in draft status",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 60,
                "status": "DRAFT",
                "mandatory": false
            }
            """;

        Object draftModuleIdObj = given()
                .contentType(ContentType.JSON)
                .body(draftModule)
                .when()
                .post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        Long draftModuleId = extractId(draftModuleIdObj);

        // Activate it
        given()
                .pathParam("id", draftModuleId)
                .contentType(ContentType.JSON)
                .body("{\"status\": \"ACTIVE\"}")
                .when()
                .patch("/api/v1/support/modules/{id}/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        // Archive it
        given()
                .pathParam("id", draftModuleId)
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
    @DisplayName("E2E Scenario 10: Agent status transition workflow")
    void testAgentStatusTransition() {
        // Create an agent
        String agentRequest = """
            {
                "employeeId": "E2E-STATUS-001",
                "name": "Status Test Agent",
                "email": "status.test@payu.id",
                "department": "Support",
                "level": "SENIOR"
            }
            """;

        Object statusAgentIdObj = given()
                .contentType(ContentType.JSON)
                .body(agentRequest)
                .when()
                .post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("active", equalTo(true))
                .extract()
                .path("id");

        Long statusAgentId = extractId(statusAgentIdObj);

        // Deactivate
        given()
                .pathParam("id", statusAgentId)
                .contentType(ContentType.JSON)
                .body("{\"active\": false}")
                .when()
                .patch("/api/v1/support/agents/{id}/status")
                .then()
                .statusCode(200)
                .body("active", equalTo(false));

        // Reactivate
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
    @Order(11)
    @DisplayName("E2E Scenario 11: Verify data consistency across endpoints")
    void testDataConsistency() {
        // Get agents list
        var agentsResponse = given()
                .when()
                .get("/api/v1/support/agents")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        // Get modules list
        var modulesResponse = given()
                .when()
                .get("/api/v1/support/modules")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        // Get trainings list
        var trainingsResponse = given()
                .when()
                .get("/api/v1/support/trainings")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        // Verify relationships exist
        assertTrue(agentsResponse.size() >= 5, "Should have at least 5 agents");
        assertTrue(modulesResponse.size() >= 4, "Should have at least 4 modules");
        assertTrue(trainingsResponse.size() >= 5, "Should have at least 5 training assignments");
    }

    @Test
    @Order(12)
    @DisplayName("E2E Scenario 12: Verify mandatory modules filtering")
    void testMandatoryModulesFiltering() {
        var mandatoryModules = given()
                .when()
                .get("/api/v1/support/modules/mandatory")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        // All returned modules should be mandatory
        for (var module : mandatoryModules) {
            Boolean isMandatory = (Boolean) ((Map<?, ?>) module).get("mandatory");
            assertTrue(isMandatory, "All modules in mandatory endpoint should be mandatory");
        }

        assertTrue(mandatoryModules.size() >= 2, "Should have at least 2 mandatory modules");
    }
}
