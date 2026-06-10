package id.payu.support.application.service;

import id.payu.support.adapter.persistence.repository.AgentTrainingRepository;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import id.payu.support.config.TestSecurityConfig;
import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.dto.CreateTrainingModuleRequest;
import id.payu.support.dto.TrainingModuleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class TrainingModuleServiceTest {

    @Autowired
    TrainingModuleService trainingModuleService;

    @Autowired
    TrainingModuleRepository trainingModuleRepository;

    @Autowired
    AgentTrainingRepository agentTrainingRepository;

    @BeforeEach
    @Transactional
    void setup() {
        agentTrainingRepository.deleteAll();
        trainingModuleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create a DRAFT training module")
    void testCreateModule() {
        CreateTrainingModuleRequest request = new CreateTrainingModuleRequest(
                "MOD-001", "KYC Basics", "Learn KYC verification",
                TrainingCategory.COMPLIANCE, 60, TrainingStatus.DRAFT, true);

        TrainingModuleResponse response = trainingModuleService.createModule(request);

        assertNotNull(response);
        assertEquals("MOD-001", response.code());
        assertEquals("KYC Basics", response.title());
        assertEquals(TrainingStatus.DRAFT, response.status());
        assertTrue(response.mandatory());
    }

    @Test
    @DisplayName("Should list all training modules")
    void testGetAllTrainingModules() {
        trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-002", "Fraud Detection", "Detect fraud patterns",
                TrainingCategory.COMPLIANCE, 90, TrainingStatus.ACTIVE, false));
        trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-003", "Customer Handling", "Handle customer complaints",
                TrainingCategory.COMMUNICATION, 45, TrainingStatus.DRAFT, true));

        var modules = trainingModuleService.getAllTrainingModules();

        assertEquals(2, modules.size());
    }

    @Test
    @DisplayName("Should get training module by ID")
    void testGetModuleById() {
        TrainingModuleResponse created = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-004", "Data Privacy", "GDPR and PDP compliance",
                TrainingCategory.COMPLIANCE, 120, TrainingStatus.ACTIVE, true));

        TrainingModuleResponse found = trainingModuleService.getModuleById(created.id());

        assertNotNull(found);
        assertEquals("MOD-004", found.code());
    }

    @Test
    @DisplayName("Should return null for non-existent module ID")
    void testGetModuleByNonExistentId() {
        TrainingModuleResponse result = trainingModuleService.getModuleById(9999L);
        assertNull(result);
    }

    @Test
    @DisplayName("Should update module status from DRAFT to ACTIVE")
    void testUpdateModuleStatus() {
        TrainingModuleResponse created = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-005", "AML Basics", "Anti-money laundering",
                TrainingCategory.COMPLIANCE, 75, TrainingStatus.DRAFT, false));

        TrainingModuleResponse updated = trainingModuleService.updateModuleStatus(created.id(), TrainingStatus.ACTIVE);

        assertNotNull(updated);
        assertEquals(TrainingStatus.ACTIVE, updated.status());
        assertEquals(created.id(), updated.id());
    }

    @Test
    @DisplayName("Should return only mandatory ACTIVE modules")
    void testGetMandatoryModules() {
        trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-006", "Mandatory Active", "Required",
                TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));
        trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-007", "Mandatory Draft", "Required draft",
                TrainingCategory.COMPLIANCE, 30, TrainingStatus.DRAFT, true));
        trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-008", "Optional Active", "Not required",
                TrainingCategory.COMMUNICATION, 30, TrainingStatus.ACTIVE, false));

        var mandatory = trainingModuleService.getMandatoryModules();

        assertEquals(1, mandatory.size());
        assertEquals("MOD-006", mandatory.getFirst().code());
    }
}
