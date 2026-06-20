import { describe, it, expect, vi, beforeEach } from 'vitest';
import SupportService, {
  type SupportAgent,
  type CreateAgentRequest,
  type TrainingModule,
  type CreateModuleRequest,
  type TrainingAssignment,
  type AssignTrainingRequest,
  type TrainingStatusSummary, // eslint-disable-line @typescript-eslint/no-unused-vars
  type CreateTicketRequest,
  type SupportTicket,
  type FAQ,
} from '@/services/SupportService';
import api from '@/lib/api';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

const mockAgent: SupportAgent = {
  id: 'agent_001',
  employeeId: 'EMP001',
  fullName: 'Siti Nurhaliza',
  email: 'siti@payu.fajjjar.my.id',
  specialization: 'ACCOUNT',
  status: 'ACTIVE',
  createdAt: '2026-02-18T10:00:00Z',
  updatedAt: '2026-02-18T10:00:00Z',
};

const mockModule: TrainingModule = {
  id: 'mod_001',
  name: 'AML Compliance Training',
  description: 'Anti-money laundering training for agents',
  category: 'COMPLIANCE',
  durationMinutes: 120,
  mandatory: true,
  status: 'ACTIVE',
  createdAt: '2026-02-18T10:00:00Z',
};

const mockAssignment: TrainingAssignment = {
  id: 'assign_001',
  agentId: 'agent_001',
  moduleId: 'mod_001',
  status: 'IN_PROGRESS',
  score: undefined,
  completedAt: undefined,
  assignedAt: '2026-02-18T10:00:00Z',
  dueDate: '2026-03-18T10:00:00Z',
};

// BUG-CROSS-060: Backend getTrainingStatus returns { activeAgents, trainedAgents, trainingPercentage }
const mockTrainingStatusOverview = {
  activeAgents: 8,
  trainedAgents: 6,
  trainingPercentage: 75.0,
};

describe('SupportService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // === Training Status ===

  // BUG-CROSS-060: getTrainingStatus returns { activeAgents, trainedAgents, trainingPercentage }
  describe('getTrainingStatus', () => {
    it('should fetch training status overview', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockTrainingStatusOverview });

      const result = await SupportService.getTrainingStatus();

      expect(api.get).toHaveBeenCalledWith('/support/training-status');
      expect(result.activeAgents).toBe(8);
      expect(result.trainingPercentage).toBe(75.0);
    });
  });

  // === Agent Management ===

  describe('listAgents', () => {
    it('should list all agents', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockAgent] });

      const result = await SupportService.listAgents();

      expect(api.get).toHaveBeenCalledWith('/support/agents');
      expect(result).toHaveLength(1);
    });
  });

  describe('createAgent', () => {
    it('should create a new agent', async () => {
      const request: CreateAgentRequest = {
        employeeId: 'EMP001',
        fullName: 'Siti Nurhaliza',
        email: 'siti@payu.fajjjar.my.id',
        specialization: 'ACCOUNT',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockAgent });

      const result = await SupportService.createAgent(request);

      expect(api.post).toHaveBeenCalledWith('/support/agents', request);
      expect(result.id).toBe('agent_001');
      expect(result.status).toBe('ACTIVE');
    });
  });

  describe('getAgent', () => {
    it('should fetch agent by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAgent });

      const result = await SupportService.getAgent('agent_001');

      expect(api.get).toHaveBeenCalledWith('/support/agents/agent_001');
      expect(result.fullName).toBe('Siti Nurhaliza');
    });
  });

  describe('getAgentByEmployeeId', () => {
    it('should fetch agent by employee ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAgent });

      const result = await SupportService.getAgentByEmployeeId('EMP001');

      expect(api.get).toHaveBeenCalledWith('/support/agents/employee/EMP001');
      expect(result.employeeId).toBe('EMP001');
    });
  });

  // BUG-CROSS-059: updateAgentStatus takes (id: number, active: boolean), sends { active }
  describe('updateAgentStatus', () => {
    it('should update agent status', async () => {
      const updatedAgent = { ...mockAgent, status: 'ON_LEAVE' as const };
      vi.mocked(api.patch).mockResolvedValue({ data: updatedAgent });

      const result = await SupportService.updateAgentStatus(1, true);

      expect(api.patch).toHaveBeenCalledWith('/support/agents/1/status', { active: true });
      expect(result.status).toBe('ON_LEAVE');
    });
  });

  // === Training Module Management ===

  describe('listModules', () => {
    it('should list all modules', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockModule] });

      const result = await SupportService.listModules();

      expect(api.get).toHaveBeenCalledWith('/support/modules');
      expect(result).toHaveLength(1);
    });
  });

  describe('getMandatoryModules', () => {
    it('should fetch mandatory modules only', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockModule] });

      const result = await SupportService.getMandatoryModules();

      expect(api.get).toHaveBeenCalledWith('/support/modules/mandatory');
      expect(result[0].mandatory).toBe(true);
    });
  });

  describe('createModule', () => {
    it('should create a training module', async () => {
      const request: CreateModuleRequest = {
        name: 'AML Compliance Training',
        description: 'Anti-money laundering training',
        category: 'COMPLIANCE',
        durationMinutes: 120,
        mandatory: true,
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockModule });

      const result = await SupportService.createModule(request);

      expect(api.post).toHaveBeenCalledWith('/support/modules', request);
      expect(result.id).toBe('mod_001');
    });
  });

  describe('getModule', () => {
    it('should fetch module by ID', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockModule });

      const result = await SupportService.getModule('mod_001');

      expect(api.get).toHaveBeenCalledWith('/support/modules/mod_001');
      expect(result.name).toBe('AML Compliance Training');
    });
  });

  describe('updateModuleStatus', () => {
    it('should update module status', async () => {
      const archivedModule = { ...mockModule, status: 'ARCHIVED' as const };
      vi.mocked(api.patch).mockResolvedValue({ data: archivedModule });

      const result = await SupportService.updateModuleStatus('mod_001', 'ARCHIVED');

      expect(api.patch).toHaveBeenCalledWith('/support/modules/mod_001/status', { status: 'ARCHIVED' });
      expect(result.status).toBe('ARCHIVED');
    });
  });

  // === Training Assignments ===

  describe('listTrainings', () => {
    it('should list all training assignments', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockAssignment] });

      const result = await SupportService.listTrainings();

      expect(api.get).toHaveBeenCalledWith('/support/trainings');
      expect(result).toHaveLength(1);
    });
  });

  // BUG-CROSS-065: Agent/module IDs are number (Long) in backend
  describe('getAgentTrainings', () => {
    it('should fetch trainings for specific agent', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockAssignment] });

      const result = await SupportService.getAgentTrainings(1);

      expect(api.get).toHaveBeenCalledWith('/support/trainings/agent/1');
      expect(result[0].agentId).toBe('agent_001');
    });
  });

  describe('getModuleTrainings', () => {
    it('should fetch trainings for specific module', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [mockAssignment] });

      const result = await SupportService.getModuleTrainings(1);

      expect(api.get).toHaveBeenCalledWith('/support/trainings/module/1');
      expect(result[0].moduleId).toBe('mod_001');
    });
  });

  describe('getAgentModuleTraining', () => {
    it('should fetch specific agent-module training', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockAssignment });

      const result = await SupportService.getAgentModuleTraining(1, 1);

      expect(api.get).toHaveBeenCalledWith('/support/trainings/agent/1/module/1');
      expect(result.status).toBe('IN_PROGRESS');
    });
  });

  // BUG-CROSS-065: AssignTrainingRequest uses number for agentId/moduleId, has status/score/notes
  describe('assignTraining', () => {
    it('should assign training to agent', async () => {
      const request: AssignTrainingRequest = {
        agentId: 1,
        moduleId: 1,
        status: 'NOT_STARTED',
        score: 0,
        notes: 'Initial assignment',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockAssignment });

      const result = await SupportService.assignTraining(request);

      expect(api.post).toHaveBeenCalledWith('/support/trainings/assign', request);
      expect(result.id).toBe('assign_001');
    });
  });

  // BUG-CROSS-061: getAgentTrainingStatus returns { agentId: number, fullyTrained: boolean }
  describe('getAgentTrainingStatus', () => {
    it('should fetch agent training status summary', async () => {
      const mockAgentStatus = { agentId: 1, fullyTrained: true };
      vi.mocked(api.get).mockResolvedValue({ data: mockAgentStatus });

      const result = await SupportService.getAgentTrainingStatus(1);

      expect(api.get).toHaveBeenCalledWith('/support/trainings/agent/1/status');
      expect(result.fullyTrained).toBe(true);
      expect(result.agentId).toBe(1);
    });
  });

  // === User-facing Ticket/FAQ ===

  describe('createTicket', () => {
    it('should create a support ticket', async () => {
      const request: CreateTicketRequest = {
        subject: 'Cannot transfer',
        description: 'Transfer keeps failing with error code TX_001',
        category: 'TRANSACTION',
        priority: 'HIGH',
      };

      const mockTicket: SupportTicket = {
        id: 'ticket_001',
        userId: 'user_123',
        subject: 'Cannot transfer',
        description: 'Transfer keeps failing',
        category: 'TRANSACTION',
        priority: 'HIGH',
        status: 'OPEN',
        createdAt: '2026-02-18T10:00:00Z',
        updatedAt: '2026-02-18T10:00:00Z',
      };

      vi.mocked(api.post).mockResolvedValue({ data: mockTicket });

      const result = await SupportService.createTicket(request);

      expect(api.post).toHaveBeenCalledWith('/support/tickets', request);
      expect(result.status).toBe('OPEN');
    });
  });

  describe('getTickets', () => {
    it('should fetch tickets filtered by status', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] });

      await SupportService.getTickets('OPEN');

      expect(api.get).toHaveBeenCalledWith('/support/tickets', { params: { status: 'OPEN' } });
    });

    it('should fetch all tickets when no status filter', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] });

      await SupportService.getTickets();

      expect(api.get).toHaveBeenCalledWith('/support/tickets', { params: { status: undefined } });
    });
  });

  describe('getFAQs', () => {
    it('should fetch FAQs with category filter', async () => {
      const mockFaqs: FAQ[] = [
        { id: 'faq_001', question: 'How to transfer?', answer: 'Go to Transfer menu...', category: 'TRANSACTION' },
      ];

      vi.mocked(api.get).mockResolvedValue({ data: mockFaqs });

      const result = await SupportService.getFAQs('TRANSACTION');

      expect(api.get).toHaveBeenCalledWith('/support/faqs', { params: { category: 'TRANSACTION' } });
      expect(result).toHaveLength(1);
    });

    it('should fetch all FAQs without filter', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: [] });

      await SupportService.getFAQs();

      expect(api.get).toHaveBeenCalledWith('/support/faqs', { params: { category: undefined } });
    });
  });
});
