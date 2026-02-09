import api from '@/lib/api';

export interface ComplianceCheck {
  id: string;
  userId: string;
  type: ComplianceCheckType;
  status: ComplianceStatus;
  result?: string;
  riskScore: number;
  checkedAt: string;
  expiresAt?: string;
}

export interface SanctionsScreening {
  id: string;
  userId: string;
  matched: boolean;
  matchDetails?: string[];
  screenedAt: string;
}

export interface RiskAssessment {
  userId: string;
  overallRisk: RiskLevel;
  transactionRisk: RiskLevel;
  identityRisk: RiskLevel;
  lastAssessedAt: string;
}

export type ComplianceCheckType = 'AML' | 'CFT' | 'SANCTIONS' | 'PEP' | 'RISK_ASSESSMENT';
export type ComplianceStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'REVIEW_REQUIRED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

class ComplianceService {
  private static instance: ComplianceService;

  static getInstance(): ComplianceService {
    if (!ComplianceService.instance) {
      ComplianceService.instance = new ComplianceService();
    }
    return ComplianceService.instance;
  }

  async getUserCompliance(userId: string): Promise<ComplianceCheck[]> {
    const response = await api.get(`/compliance/users/${userId}/checks`);
    return response.data;
  }

  async getRiskAssessment(userId: string): Promise<RiskAssessment> {
    const response = await api.get(`/compliance/users/${userId}/risk`);
    return response.data;
  }

  async screenForSanctions(userId: string): Promise<SanctionsScreening> {
    const response = await api.post(`/compliance/users/${userId}/sanctions-screening`);
    return response.data;
  }

  async getComplianceStatus(): Promise<{ compliant: boolean; pendingChecks: number }> {
    const response = await api.get('/compliance/status');
    return response.data;
  }
}

export default ComplianceService.getInstance();
