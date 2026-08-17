package id.payu.support.application.service;

import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.adapter.persistence.repository.AgentTrainingRepository;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import id.payu.support.adapter.persistence.repository.TrainingModuleRepository;
import id.payu.support.config.TestSecurityConfig;
import id.payu.support.domain.AgentLevel;
import id.payu.support.domain.CompletionStatus;
import id.payu.support.domain.TrainingCategory;
import id.payu.support.domain.TrainingStatus;
import id.payu.support.interfaces.dto.*;
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
class AgentTrainingServiceTest {

    @Autowired
    AgentTrainingService agentTrainingService;

    @Autowired
    AgentService agentService;

    @Autowired
    TrainingModuleService trainingModuleService;

    @Autowired
    SupportAgentRepository agentRepository;

    @Autowired
    TrainingModuleRepository moduleRepository;

    @Autowired
    AgentTrainingRepository agentTrainingRepository;

    @BeforeEach
    @Transactional
    void setup() {
        agentTrainingRepository.deleteAll();
        moduleRepository.deleteAll();
        agentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should assign training with IN_PROGRESS status")
    void testAssignTraining() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP101", "Training Agent", "train@payu.fajjjar.my.id",
                "Customer Support", AgentLevel.JUNIOR));
        TrainingModuleResponse module = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-101", "Onboarding", "New agent onboarding",
                TrainingCategory.ONBOARDING, 30, TrainingStatus.ACTIVE, true));

        AgentTrainingResponse training = agentTrainingService.assignTraining(new AssignTrainingRequest(
                agent.id(), module.id(), CompletionStatus.IN_PROGRESS, 0, "Starting training"));

        assertNotNull(training);
        assertEquals(agent.id(), training.agentId());
        assertEquals(module.id(), training.trainingModuleId());
        assertEquals(CompletionStatus.IN_PROGRESS, training.status());
    }

    @Test
    @DisplayName("Should list all agent trainings")
    void testGetAllAgentTrainings() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP102", "Agent 102", "a102@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod1 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-102", "Module A", "Desc A", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, false));
        TrainingModuleResponse mod2 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-103", "Module B", "Desc B", TrainingCategory.COMMUNICATION, 45, TrainingStatus.ACTIVE, false));

        agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), mod1.id(), CompletionStatus.IN_PROGRESS, 0, null));
        agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), mod2.id(), CompletionStatus.PASSED, 85, null));

        var all = agentTrainingService.getAllAgentTrainings();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("Should filter trainings by agent ID")
    void testGetTrainingsByAgent() {
        AgentResponse agent1 = agentService.createAgent(new CreateAgentRequest(
                "EMP103", "Agent 103", "a103@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        AgentResponse agent2 = agentService.createAgent(new CreateAgentRequest(
                "EMP104", "Agent 104", "a104@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod1 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-104", "Security 101", "Basics", TrainingCategory.COMPLIANCE, 60, TrainingStatus.ACTIVE, true));
        TrainingModuleResponse mod2 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-105", "Advanced Security", "Advanced", TrainingCategory.COMPLIANCE, 90, TrainingStatus.ACTIVE, true));

        agentTrainingService.assignTraining(new AssignTrainingRequest(agent1.id(), mod1.id(), CompletionStatus.PASSED, 90, null));
        agentTrainingService.assignTraining(new AssignTrainingRequest(agent1.id(), mod2.id(), CompletionStatus.IN_PROGRESS, 0, null));
        agentTrainingService.assignTraining(new AssignTrainingRequest(agent2.id(), mod1.id(), CompletionStatus.IN_PROGRESS, 0, null));

        var agent1Trainings = agentTrainingService.getTrainingsByAgent(agent1.id());

        assertEquals(2, agent1Trainings.size());
    }

    @Test
    @DisplayName("Should re-assign training to update status")
    void testAssignTrainingUpdatesExisting() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP105", "Agent 105", "a105@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-106", "Update Test", "Desc", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));

        AgentTrainingResponse first = agentTrainingService.assignTraining(new AssignTrainingRequest(
                agent.id(), mod.id(), CompletionStatus.IN_PROGRESS, 0, null));
        AgentTrainingResponse second = agentTrainingService.assignTraining(new AssignTrainingRequest(
                agent.id(), mod.id(), CompletionStatus.PASSED, 95, "Mastered"));

        assertEquals(first.id(), second.id());
        assertEquals(CompletionStatus.PASSED, second.status());
        assertEquals(95, second.score());
        assertEquals("Mastered", second.notes());
    }

    @Test
    @DisplayName("Should detect fully trained agent")
    void testIsAgentFullyTrained() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP106", "Agent 106", "a106@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod1 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-107", "Mandatory 1", "Required", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));
        TrainingModuleResponse mod2 = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-108", "Mandatory 2", "Required", TrainingCategory.COMPLIANCE, 45, TrainingStatus.ACTIVE, true));

        agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), mod1.id(), CompletionStatus.PASSED, 80, null));
        agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), mod2.id(), CompletionStatus.PASSED, 85, null));

        assertTrue(agentTrainingService.isAgentFullyTrained(agent.id()));
    }

    @Test
    @DisplayName("Should detect agent not fully trained with IN_PROGRESS module")
    void testIsAgentNotFullyTrained() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP107", "Agent 107", "a107@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-109", "Incomplete Module", "Required", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));

        agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), mod.id(), CompletionStatus.IN_PROGRESS, 0, null));

        assertFalse(agentTrainingService.isAgentFullyTrained(agent.id()));
    }

    @Test
    @DisplayName("Should count fully trained agents")
    void testCountFullyTrainedAgents() {
        AgentResponse agent1 = agentService.createAgent(new CreateAgentRequest(
                "EMP108", "Fully Trained", "ft@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        AgentResponse agent2 = agentService.createAgent(new CreateAgentRequest(
                "EMP109", "Partially Trained", "pt@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));
        TrainingModuleResponse mod = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-110", "Sole Mandatory", "Required", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));

        agentTrainingService.assignTraining(new AssignTrainingRequest(agent1.id(), mod.id(), CompletionStatus.PASSED, 80, null));
        agentTrainingService.assignTraining(new AssignTrainingRequest(agent2.id(), mod.id(), CompletionStatus.IN_PROGRESS, 0, null));

        long count = agentTrainingService.countFullyTrainedAgents();
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should throw when assigning training to non-existent agent")
    void testAssignTrainingAgentNotFound() {
        TrainingModuleResponse mod = trainingModuleService.createModule(new CreateTrainingModuleRequest(
                "MOD-111", "Orphan Module", "Desc", TrainingCategory.COMPLIANCE, 30, TrainingStatus.ACTIVE, true));

        assertThrows(RuntimeException.class, () ->
                agentTrainingService.assignTraining(new AssignTrainingRequest(9999L, mod.id(), CompletionStatus.IN_PROGRESS, 0, null)));
    }

    @Test
    @DisplayName("Should throw when assigning training to non-existent module")
    void testAssignTrainingModuleNotFound() {
        AgentResponse agent = agentService.createAgent(new CreateAgentRequest(
                "EMP110", "Orphan Agent", "oa@payu.fajjjar.my.id",
                "Support", AgentLevel.JUNIOR));

        assertThrows(RuntimeException.class, () ->
                agentTrainingService.assignTraining(new AssignTrainingRequest(agent.id(), 9999L, CompletionStatus.IN_PROGRESS, 0, null)));
    }
}
