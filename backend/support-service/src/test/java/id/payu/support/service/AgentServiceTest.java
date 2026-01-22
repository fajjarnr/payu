package id.payu.support.service;

import id.payu.support.domain.AgentTraining;
import id.payu.support.domain.SupportAgent;
import id.payu.support.dto.AgentResponse;
import id.payu.support.dto.CreateAgentRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AgentServiceTest {

    @Inject
    AgentService agentService;

    @BeforeEach
    @jakarta.transaction.Transactional
    void setup() {
        AgentTraining.deleteAll();
        SupportAgent.deleteAll();
    }

    @Test
    void testCreateAgent() {
        CreateAgentRequest request = new CreateAgentRequest(
                "EMP999",
                "Test Agent",
                "test@payu.id",
                "Customer Support",
                SupportAgent.AgentLevel.JUNIOR
        );

        AgentResponse response = agentService.createAgent(request);

        assertNotNull(response);
        assertEquals("EMP999", response.employeeId());
        assertEquals("Test Agent", response.name());
        assertEquals("test@payu.id", response.email());
        assertEquals(SupportAgent.AgentLevel.JUNIOR, response.level());
        assertTrue(response.active());
    }

    @Test
    void testGetAgentByEmployeeId() {
        CreateAgentRequest request = new CreateAgentRequest(
                "EMP998",
                "Test Agent 2",
                "test2@payu.id",
                "Customer Support",
                SupportAgent.AgentLevel.SENIOR
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
                "test3@payu.id",
                "Customer Support",
                SupportAgent.AgentLevel.JUNIOR
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
                "active@payu.id",
                "Customer Support",
                SupportAgent.AgentLevel.JUNIOR
        );

        CreateAgentRequest request2 = new CreateAgentRequest(
                "EMP995",
                "Inactive Agent",
                "inactive@payu.id",
                "Customer Support",
                SupportAgent.AgentLevel.JUNIOR
        );

        agentService.createAgent(request1);
        AgentResponse agent2 = agentService.createAgent(request2);
        agentService.updateAgentStatus(agent2.id(), false);

        long count = agentService.countActiveAgents();
        assertEquals(1, count);
    }
}
