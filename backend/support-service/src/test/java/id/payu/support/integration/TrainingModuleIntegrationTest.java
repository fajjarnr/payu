package id.payu.support.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.support.config.TestSecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Training Module workflows. Uses MockMvc to avoid
 * RestAssured HTTPBuilder NPE on Java 25 (per L-064).
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrainingModuleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static Long createdModuleId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new training module successfully")
    void testCreateTrainingModule() throws Exception {
        String requestBody = """
            {
                "code": "INT-TEST-001",
                "title": "Integration Test Module",
                "description": "Module for integration testing",
                "category": "PRODUCT_KNOWLEDGE",
                "durationMinutes": 60,
                "mandatory": true
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/support/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Integration Test Module"))
                .andExpect(jsonPath("$.data.durationMinutes").value(60))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        createdModuleId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve module by ID")
    void testGetModuleById() throws Exception {
        Assumptions.assumeTrue(createdModuleId != null, "Module must be created first");

        mockMvc.perform(get("/api/v1/support/modules/{id}", createdModuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdModuleId.intValue()))
                .andExpect(jsonPath("$.data.title").value("Integration Test Module"));
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve all mandatory modules")
    void testGetMandatoryModules() throws Exception {
        // Create + activate a mandatory module for this test
        String createBody = """
            {
                "code": "INT-TEST-MAND-001",
                "title": "Mandatory Module",
                "description": "Module mandatory for all agents",
                "category": "COMPLIANCE",
                "durationMinutes": 30,
                "mandatory": true
            }
            """;
        MvcResult createResult = mockMvc.perform(post("/api/v1/support/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        Long newModuleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Activate the module (mandatory filter requires status=ACTIVE)
        mockMvc.perform(patch("/api/v1/support/modules/{id}/status", newModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        // Now query mandatory modules
        mockMvc.perform(get("/api/v1/support/modules/mandatory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Should update module status to ACTIVE")
    void testUpdateModuleStatus() throws Exception {
        Assumptions.assumeTrue(createdModuleId != null, "Module must be created first");

        mockMvc.perform(patch("/api/v1/support/modules/{id}/status", createdModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent module")
    void testGetNonExistentModule() throws Exception {
        mockMvc.perform(get("/api/v1/support/modules/{id}", 99999))
                .andExpect(status().isNotFound());
    }
}
