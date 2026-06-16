package id.payu.support.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.support.config.TestSecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Integration tests for Support Service REST endpoints.
 * Uses full @SpringBootTest with WebApplicationContext to enable Spring Security
 * filter chain (TestSecurityConfig). Avoids RestAssured HTTPBuilder NPE on Java 25.
 *
 * Security: TestSecurityConfig (permitAll) bypasses JWT for tests.
 * Live security verified by E2E blackbox tests in tests/e2e_blackbox/test_support_flow.py.
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupportResourceTest {

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static Long agentId;
    private static Long moduleId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    void testGetTrainingStatus() throws Exception {
        mockMvc.perform(get("/api/v1/support/training-status"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").exists());
    }

    @Test
    @Order(2)
    void testGetAllAgents() throws Exception {
        mockMvc.perform(get("/api/v1/support/agents"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(3)
    void testCreateAgent() throws Exception {
        String request = """
            {
                "employeeId": "EMP9999",
                "name": "Integration Test Agent",
                "email": "integration@payu.fajjjar.my.id",
                "department": "QA",
                "level": "JUNIOR"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/support/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.employeeId").value("EMP9999"))
                .andReturn();

        agentId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @Order(4)
    void testGetAgentById() throws Exception {
        Assumptions.assumeTrue(agentId != null, "agentId required from prior testCreateAgent");

        mockMvc.perform(get("/api/v1/support/agents/{id}", agentId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.id").value(agentId.intValue()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.employeeId").value("EMP9999"));
    }

    @Test
    @Order(5)
    void testCreateTrainingModule() throws Exception {
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

        MvcResult result = mockMvc.perform(post("/api/v1/support/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.code").value("TEST-001"))
                .andReturn();

        moduleId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @Order(6)
    void testGetAllModules() throws Exception {
        mockMvc.perform(get("/api/v1/support/modules"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(7)
    void testAssignTraining() throws Exception {
        Assumptions.assumeTrue(agentId != null && moduleId != null,
                "agentId and moduleId required from prior tests");

        String request = """
            {
                "agentId": %d,
                "moduleId": %d,
                "status": "IN_PROGRESS",
                "notes": "Started integration test"
            }
            """.formatted(agentId, moduleId);

        mockMvc.perform(post("/api/v1/support/trainings/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.is(201), org.hamcrest.Matchers.is(200))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(8)
    void testGetTrainingsByAgent() throws Exception {
        Assumptions.assumeTrue(agentId != null, "agentId required from prior testCreateAgent");

        mockMvc.perform(get("/api/v1/support/trainings/agent/{agentId}", agentId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Order(9)
    void testGetAllTrainings() throws Exception {
        mockMvc.perform(get("/api/v1/support/trainings"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
