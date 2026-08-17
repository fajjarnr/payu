package id.payu.support.application.service;

import id.payu.support.adapter.persistence.repository.AgentTrainingRepository;
import id.payu.support.adapter.persistence.repository.SupportAgentRepository;
import id.payu.support.adapter.persistence.entity.SupportAgentEntity;
import id.payu.support.domain.AgentLevel;
import id.payu.support.interfaces.dto.AgentResponse;
import id.payu.support.interfaces.dto.CreateAgentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AgentServiceTest {

    @Autowired
    AgentService agentService;

    @Autowired
    SupportAgentRepository supportAgentRepository;

    @Autowired
    AgentTrainingRepository agentTrainingRepository;

    @BeforeEach
    @Transactional
    void setup() {
        agentTrainingRepository.deleteAll();
        supportAgentRepository.deleteAll();
    }

    @Test
    void testCreateAgent() {
        CreateAgentRequest request = new CreateAgentRequest(
                "EMP999",
                "Test Agent",
                "test@payu.fajjjar.my.id",
                "Customer Support",
                AgentLevel.JUNIOR
        );

        AgentResponse response = agentService.createAgent(request);

        assertNotNull(response);
        assertEquals("EMP999", response.employeeId());
        assertEquals("Test Agent", response.name());
        assertEquals("test@payu.fajjjar.my.id", response.email());
        assertEquals(AgentLevel.JUNIOR, response.level());
        assertTrue(response.active());
    }

    @Test
    void testGetAgentByEmployeeId() {
        CreateAgentRequest request = new CreateAgentRequest(
                "EMP998",
                "Test Agent 2",
                "test2@payu.fajjjar.my.id",
                "Customer Support",
                AgentLevel.SENIOR
        );

        agentService.createAgent(request);
        AgentResponse response = agentService.getAgentByEmployeeId("EMP998");

        assertNotNull(response);
        assertEquals("EMP998", response.employeeId());
        assertEquals("Test Agent 2", response.name());
    }

    @Test
    void testUpdateAgentStatus() {
        CreateAgentRequest request = new CreateAgentRequest(
                "EMP997",
                "Test Agent 3",
                "test3@payu.fajjjar.my.id",
                "Customer Support",
                AgentLevel.JUNIOR
        );

        AgentResponse created = agentService.createAgent(request);
        assertTrue(created.active());

        AgentResponse updated = agentService.updateAgentStatus(created.id(), false);
        assertNotNull(updated);
        assertFalse(updated.active());
    }

    @Test
    void testCountActiveAgents() {
        CreateAgentRequest request1 = new CreateAgentRequest(
                "EMP996",
                "Active Agent",
                "active@payu.fajjjar.my.id",
                "Customer Support",
                AgentLevel.JUNIOR
        );

        CreateAgentRequest request2 = new CreateAgentRequest(
                "EMP995",
                "Inactive Agent",
                "inactive@payu.fajjjar.my.id",
                "Customer Support",
                AgentLevel.JUNIOR
        );

        agentService.createAgent(request1);
        AgentResponse agent2 = agentService.createAgent(request2);
        agentService.updateAgentStatus(agent2.id(), false);

        long count = agentService.countActiveAgents();
        assertEquals(1, count);
    }
}
