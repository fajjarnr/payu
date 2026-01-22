package id.payu.support.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupportResourceTest {

    private static Long agentId;
    private static Long moduleId;

    @Test
    @Order(1)
    void testGetTrainingStatus() {
        given()
                .when().get("/api/v1/support/training-status")
                .then()
                .statusCode(200)
                .body("$", hasKey("activeAgents"))
                .body("$", hasKey("trainedAgents"))
                .body("$", hasKey("trainingPercentage"));
    }

    @Test
    @Order(2)
    void testGetAllAgents() {
        given()
                .when().get("/api/v1/support/agents")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    void testCreateAgent() {
        String request = """
            {
                "employeeId": "EMP9999",
                "name": "Integration Test Agent",
                "email": "integration@payu.id",
                "department": "QA",
                "level": "JUNIOR"
            }
            """;

        Integer id = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/v1/support/agents")
                .then()
                .statusCode(201)
                .body("employeeId", equalTo("EMP9999"))
                .body("name", equalTo("Integration Test Agent"))
                .extract().path("id");

        agentId = id != null ? id.longValue() : null;
    }

    @Test
    @Order(4)
    void testGetAgentById() {
        if (agentId == null) return;

        given()
                .pathParam("id", agentId)
                .when().get("/api/v1/support/agents/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(agentId.intValue()))
                .body("employeeId", equalTo("EMP9999"));
    }

    @Test
    @Order(5)
    void testCreateTrainingModule() {
        String request = """
            {
                "code": "TEST-001",
                "title": "Test Training Module",
                "description": "A test training module for integration testing",
                "category": "ONBOARDING",
                "durationMinutes": 60,
                "status": "ACTIVE",
                "mandatory": false
            }
            """;

        Integer id = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/v1/support/modules")
                .then()
                .statusCode(201)
                .body("code", equalTo("TEST-001"))
                .body("title", equalTo("Test Training Module"))
                .extract().path("id");

        moduleId = id != null ? id.longValue() : null;
    }

    @Test
    @Order(6)
    void testGetAllModules() {
        given()
                .when().get("/api/v1/support/modules")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    void testAssignTraining() {
        if (agentId == null || moduleId == null) return;

        String request = """
            {
                "agentId": %d,
                "trainingModuleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Started integration test"
            }
            """.formatted(agentId, moduleId);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/v1/support/trainings/assign")
                .then()
                .statusCode(anyOf(is(201), is(200)))
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @Order(8)
    void testGetTrainingsByAgent() {
        if (agentId == null) return;

        given()
                .pathParam("agentId", agentId)
                .when().get("/api/v1/support/trainings/agent/{agentId}")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(9)
    void testGetAllTrainings() {
        given()
                .when().get("/api/v1/support/trainings")
                .then()
                .statusCode(200);
    }
}
