import api from '@/lib/api';

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

  async createTicket(request: CreateTicketRequest): Promise<SupportTicket> {
    const response = await api.post('/support/tickets', request);
    return response.data;
  }

  async getTickets(status?: TicketStatus): Promise<SupportTicket[]> {
    const response = await api.get('/support/tickets', { params: { status } });
    return response.data;
  }

  async getTicket(ticketId: string): Promise<SupportTicket> {
    const response = await api.get(`/support/tickets/${ticketId}`);
    return response.data;
  }

  async getMessages(ticketId: string): Promise<TicketMessage[]> {
    const response = await api.get(`/support/tickets/${ticketId}/messages`);
    return response.data;
  }

  async sendMessage(ticketId: string, message: string): Promise<TicketMessage> {
    const response = await api.post(`/support/tickets/${ticketId}/messages`, { message });
    return response.data;
  }

  async getFAQs(category?: string): Promise<FAQ[]> {
    const response = await api.get('/support/faqs', { params: { category } });
    return response.data;
  }
}

export default SupportService.getInstance();
