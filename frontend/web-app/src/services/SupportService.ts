import api from '@/lib/api';

// --- Interfaces matching backend SupportController (Spring Boot) ---
// Backend provides: agent management, training modules, training assignments

export interface SupportAgent {
  id: string;
  employeeId: string;
  fullName: string;
  email: string;
  specialization: string;
  status: AgentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAgentRequest {
  employeeId: string;
  fullName: string;
  email: string;
  specialization: string;
}

export interface TrainingModule {
  id: string;
  name: string;
  description: string;
  category: string;
  durationMinutes: number;
  mandatory: boolean;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  createdAt: string;
}

export interface CreateModuleRequest {
  name: string;
  description: string;
  category: string;
  durationMinutes: number;
  mandatory: boolean;
}

export interface TrainingAssignment {
  id: string;
  agentId: string;
  moduleId: string;
  status: TrainingStatus;
  score?: number;
  completedAt?: string;
  assignedAt: string;
  dueDate?: string;
}

export interface AssignTrainingRequest {
  agentId: string;
  moduleId: string;
  dueDate?: string;
}

export interface TrainingStatusSummary {
  agentId: string;
  totalModules: number;
  completedModules: number;
  inProgressModules: number;
  overdue: number;
  complianceRate: number;
}

export type AgentStatus = 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE' | 'SUSPENDED';
export type TrainingStatus = 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'OVERDUE';

// --- User-facing ticket types (future backend endpoint) ---
export interface SupportTicket {
  id: string;
  userId: string;
  subject: string;
  description: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  assignedTo?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}
export interface TicketMessage {
  id: string;
  ticketId: string;
  sender: 'USER' | 'AGENT' | 'SYSTEM';
  message: string;
  attachments?: string[];
  sentAt: string;
}
export interface CreateTicketRequest {
  subject: string;
  description: string;
  category: TicketCategory;
  priority?: TicketPriority;
}
export interface FAQ {
  id: string;
  question: string;
  answer: string;
  category: string;
}
export type TicketCategory = 'ACCOUNT' | 'TRANSACTION' | 'CARD' | 'LOAN' | 'TECHNICAL' | 'OTHER';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_CUSTOMER' | 'RESOLVED' | 'CLOSED';

class SupportService {
  private static instance: SupportService;

  static getInstance(): SupportService {
    if (!SupportService.instance) {
      SupportService.instance = new SupportService();
    }
    return SupportService.instance;
  }

  // === Training Status (overview) ===

  /** GET /support/training-status */
  async getTrainingStatus(): Promise<TrainingStatusSummary[]> {
    const response = await api.get('/support/training-status');
    return response.data;
  }

  // === Agent Management ===

  /** GET /support/agents */
  async listAgents(): Promise<SupportAgent[]> {
    const response = await api.get('/support/agents');
    return response.data;
  }

  /** POST /support/agents */
  async createAgent(request: CreateAgentRequest): Promise<SupportAgent> {
    const response = await api.post('/support/agents', request);
    return response.data;
  }

  /** GET /support/agents/{id} */
  async getAgent(id: string): Promise<SupportAgent> {
    const response = await api.get(`/support/agents/${id}`);
    return response.data;
  }

  /** GET /support/agents/employee/{employeeId} */
  async getAgentByEmployeeId(employeeId: string): Promise<SupportAgent> {
    const response = await api.get(`/support/agents/employee/${employeeId}`);
    return response.data;
  }

  /** PATCH /support/agents/{id}/status */
  async updateAgentStatus(id: string, status: AgentStatus): Promise<SupportAgent> {
    const response = await api.patch(`/support/agents/${id}/status`, { status });
    return response.data;
  }

  // === Training Module Management ===

  /** GET /support/modules */
  async listModules(): Promise<TrainingModule[]> {
    const response = await api.get('/support/modules');
    return response.data;
  }

  /** GET /support/modules/mandatory */
  async getMandatoryModules(): Promise<TrainingModule[]> {
    const response = await api.get('/support/modules/mandatory');
    return response.data;
  }

  /** POST /support/modules */
  async createModule(request: CreateModuleRequest): Promise<TrainingModule> {
    const response = await api.post('/support/modules', request);
    return response.data;
  }

  /** GET /support/modules/{id} */
  async getModule(id: string): Promise<TrainingModule> {
    const response = await api.get(`/support/modules/${id}`);
    return response.data;
  }

  /** PATCH /support/modules/{id}/status */
  async updateModuleStatus(id: string, status: string): Promise<TrainingModule> {
    const response = await api.patch(`/support/modules/${id}/status`, { status });
    return response.data;
  }

  // === Training Assignments ===

  /** GET /support/trainings */
  async listTrainings(): Promise<TrainingAssignment[]> {
    const response = await api.get('/support/trainings');
    return response.data;
  }

  /** GET /support/trainings/agent/{agentId} */
  async getAgentTrainings(agentId: string): Promise<TrainingAssignment[]> {
    const response = await api.get(`/support/trainings/agent/${agentId}`);
    return response.data;
  }

  /** GET /support/trainings/module/{moduleId} */
  async getModuleTrainings(moduleId: string): Promise<TrainingAssignment[]> {
    const response = await api.get(`/support/trainings/module/${moduleId}`);
    return response.data;
  }

  /** GET /support/trainings/agent/{agentId}/module/{moduleId} */
  async getAgentModuleTraining(agentId: string, moduleId: string): Promise<TrainingAssignment> {
    const response = await api.get(`/support/trainings/agent/${agentId}/module/${moduleId}`);
    return response.data;
  }

  /** POST /support/trainings/assign */
  async assignTraining(request: AssignTrainingRequest): Promise<TrainingAssignment> {
    const response = await api.post('/support/trainings/assign', request);
    return response.data;
  }

  /** GET /support/trainings/agent/{agentId}/status */
  async getAgentTrainingStatus(agentId: string): Promise<TrainingStatusSummary> {
    const response = await api.get(`/support/trainings/agent/${agentId}/status`);
    return response.data;
  }

  // === User-facing ticket/FAQ endpoints (pending backend implementation) ===

  /** POST /support/tickets — Create support ticket (future) */
  async createTicket(request: CreateTicketRequest): Promise<SupportTicket> {
    const response = await api.post('/support/tickets', request);
    return response.data;
  }

  /** GET /support/tickets — List user tickets (future) */
  async getTickets(status?: TicketStatus): Promise<SupportTicket[]> {
    const response = await api.get('/support/tickets', { params: { status } });
    return response.data;
  }

  /** GET /support/faqs — Get FAQs (future) */
  async getFAQs(category?: string): Promise<FAQ[]> {
    const response = await api.get('/support/faqs', { params: { category } });
    return response.data;
  }
}

export default SupportService.getInstance();
