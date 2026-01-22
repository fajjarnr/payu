import api from '@/lib/api';

export enum KycStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  REQUIRES_ADDITIONAL_INFO = 'REQUIRES_ADDITIONAL_INFO'
}

export interface KycReviewResponse {
  id: string;
  userId: string;
  accountNumber: string;
  documentType: string;
  documentNumber: string;
  documentUrl: string;
  fullName: string;
  address: string;
  phoneNumber: string;
  status: KycStatus;
  notes: string;
  reviewedBy: string;
  reviewedAt: string;
  createdAt: string;
}

export interface KycReviewDecisionRequest {
  status: KycStatus;
  notes?: string;
}

export enum FraudRiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum FraudCaseStatus {
  OPEN = 'OPEN',
  UNDER_INVESTIGATION = 'UNDER_INVESTIGATION',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED',
  FALSE_POSITIVE = 'FALSE_POSITIVE'
}

export interface FraudCaseResponse {
  id: string;
  userId: string;
  accountNumber: string;
  transactionId: string;
  transactionType: string;
  amount: number;
  fraudType: string;
  riskLevel: FraudRiskLevel;
  status: FraudCaseStatus;
  description: string;
  evidence: string;
  notes: string;
  assignedTo: string;
  resolvedBy: string;
  resolvedAt: string;
  createdAt: string;
}

export interface FraudCaseDecisionRequest {
  status: FraudCaseStatus;
  notes?: string;
}

export enum CustomerCasePriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export enum CustomerCaseStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  WAITING_FOR_CUSTOMER = 'WAITING_FOR_CUSTOMER',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED'
}

export enum CustomerCaseType {
  INQUIRY = 'INQUIRY',
  COMPLAINT = 'COMPLAINT',
  DISPUTE = 'DISPUTE',
  REQUEST = 'REQUEST'
}

export interface CustomerCaseResponse {
  id: string;
  userId: string;
  accountNumber: string;
  caseNumber: string;
  caseType: CustomerCaseType;
  priority: CustomerCasePriority;
  subject: string;
  description: string;
  status: CustomerCaseStatus;
  notes: string;
  assignedTo: string;
  resolvedBy: string;
  resolvedAt: string;
  createdAt: string;
}

export interface CustomerCaseUpdateRequest {
  status?: CustomerCaseStatus;
  priority?: CustomerCasePriority;
  notes?: string;
  assignedTo?: string;
}

export class BackofficeService {
  private static instance: BackofficeService;

  private constructor() { }

  static getInstance(): BackofficeService {
    if (!BackofficeService.instance) {
      BackofficeService.instance = new BackofficeService();
    }
    return BackofficeService.instance;
  }

  // KYC Reviews
  async getKycReviews(status?: string, page: number = 0, size: number = 20): Promise<KycReviewResponse[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    const response = await api.get<KycReviewResponse[]>(`/backoffice/kyc-reviews?${params.toString()}`);
    return response.data;
  }

  async getKycReview(id: string): Promise<KycReviewResponse> {
    const response = await api.get<KycReviewResponse>(`/backoffice/kyc-reviews/${id}`);
    return response.data;
  }

  async reviewKyc(id: string, decision: KycReviewDecisionRequest): Promise<KycReviewResponse> {
    const response = await api.post<KycReviewResponse>(`/backoffice/kyc-reviews/${id}/review`, decision);
    return response.data;
  }

  // Fraud Cases
  async getFraudCases(status?: string, riskLevel?: string, page: number = 0, size: number = 20): Promise<FraudCaseResponse[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (riskLevel) params.append('riskLevel', riskLevel);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await api.get<FraudCaseResponse[]>(`/backoffice/fraud-cases?${params.toString()}`);
    return response.data;
  }

  async getFraudCase(id: string): Promise<FraudCaseResponse> {
    const response = await api.get<FraudCaseResponse>(`/backoffice/fraud-cases/${id}`);
    return response.data;
  }

  async resolveFraudCase(id: string, decision: FraudCaseDecisionRequest): Promise<FraudCaseResponse> {
    const response = await api.post<FraudCaseResponse>(`/backoffice/fraud-cases/${id}/resolve`, decision);
    return response.data;
  }

  async assignFraudCase(id: string, assignedTo: string): Promise<FraudCaseResponse> {
    const params = new URLSearchParams();
    params.append('assignedTo', assignedTo);
    const response = await api.post<FraudCaseResponse>(`/backoffice/fraud-cases/${id}/assign`, params);
    return response.data;
  }

  // Customer Cases
  async getCustomerCases(status?: string, priority?: string, page: number = 0, size: number = 20): Promise<CustomerCaseResponse[]> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (priority) params.append('priority', priority);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await api.get<CustomerCaseResponse[]>(`/backoffice/customer-cases?${params.toString()}`);
    return response.data;
  }

  async getCustomerCase(id: string): Promise<CustomerCaseResponse> {
    const response = await api.get<CustomerCaseResponse>(`/backoffice/customer-cases/${id}`);
    return response.data;
  }

  async updateCustomerCase(id: string, update: CustomerCaseUpdateRequest): Promise<CustomerCaseResponse> {
    const response = await api.put<CustomerCaseResponse>(`/backoffice/customer-cases/${id}`, update);
    return response.data;
  }

  async assignCustomerCase(id: string, assignedTo: string): Promise<CustomerCaseResponse> {
    const params = new URLSearchParams();
    params.append('assignedTo', assignedTo);
    const response = await api.post<CustomerCaseResponse>(`/backoffice/customer-cases/${id}/assign`, params);
    return response.data;
  }
}

export default BackofficeService.getInstance();
