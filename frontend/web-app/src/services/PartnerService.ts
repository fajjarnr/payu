import api from '@/lib/api';

export interface Partner {
  id: number;
  name: string;
  type: string;
  email: string;
  phone: string;
  clientId?: string;
  // BUG-FE-032: clientSecret removed to prevent accidental exposure
  publicKey?: string;
  active: boolean;
}

export interface PartnerWithCredentials extends Partner {
  clientSecret: string;
}

export interface Certificate {
  id: string;
  partnerId: number;
  subject: string;
  issuer: string;
  serialNumber: string;
  validFrom: string;
  validTo: string;
  status: 'ACTIVE' | 'INACTIVE' | 'EXPIRED' | 'REVOKED';
  fingerprint: string;
  createdAt: string;
}

export interface SnapBiPayment {
  id: string;
  partnerId: number;
  amount: number;
  currency: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  referenceId: string;
  createdAt: string;
}

export const PartnerService = {
  // === Partner CRUD ===

  /** GET /partners — List all partners */
  async listPartners() {
    const response = await api.get<Partner[]>('/partners');
    return response.data;
  },

  /** POST /partners — Register new partner */
  async register(data: { name: string; email: string; type: string; phone: string; publicKey?: string }) {
    const response = await api.post<PartnerWithCredentials>('/partners', data);
    return response.data;
  },

  /** GET /partners/{id} — Get partner profile */
  async getProfile(id: number) {
    const response = await api.get<Partner>(`/partners/${id}`);
    return response.data;
  },

  /** GET /partners/me — Get my partner by JWT email */
  async getMyPartner() {
    const response = await api.get<Partner>('/partners/me');
    return response.data;
  },

  /** PUT /partners/{id} — Update partner */
  async updatePartner(id: number, data: Partial<Partner>) {
    const response = await api.put<Partner>(`/partners/${id}`, data);
    return response.data;
  },

  /** POST /partners/{id}/keys/regenerate — Regenerate API keys */
  async regenerateKeys(id: number) {
    const response = await api.post<PartnerWithCredentials>(`/partners/${id}/keys/regenerate`);
    return response.data;
  },

  /** DELETE /partners/{id} — Delete partner */
  async deletePartner(id: number) {
    await api.delete(`/partners/${id}`);
  },

  // === Certificate Management ===

  /** GET /partners/{partnerId}/certificates */
  async getCertificates(partnerId: number) {
    const response = await api.get<Certificate[]>(`/partners/${partnerId}/certificates`);
    return response.data;
  },

  /** GET /partners/{partnerId}/certificates/active */
  async getActiveCertificates(partnerId: number) {
    const response = await api.get<Certificate[]>(`/partners/${partnerId}/certificates/active`);
    return response.data;
  },

  /** GET /partners/{partnerId}/certificates/valid */
  async getValidCertificates(partnerId: number) {
    const response = await api.get<Certificate[]>(`/partners/${partnerId}/certificates/valid`);
    return response.data;
  },

  /** GET /partners/{partnerId}/certificates/expiring */
  async getExpiringCertificates(partnerId: number) {
    const response = await api.get<Certificate[]>(`/partners/${partnerId}/certificates/expiring`);
    return response.data;
  },

  /** POST /partners/{partnerId}/certificates — Upload certificate */
  async uploadCertificate(partnerId: number, certData: string) {
    const response = await api.post<Certificate>(`/partners/${partnerId}/certificates`, { certificate: certData });
    return response.data;
  },

  /** POST /partners/{partnerId}/certificates/generate — Generate certificate */
  async generateCertificate(partnerId: number) {
    const response = await api.post<Certificate>(`/partners/${partnerId}/certificates/generate`);
    return response.data;
  },

  /** GET /partners/{partnerId}/certificates/{certificateId}/validate */
  async validateCertificate(partnerId: number, certificateId: string) {
    const response = await api.get<{ valid: boolean; details: string }>(`/partners/${partnerId}/certificates/${certificateId}/validate`);
    return response.data;
  },

  /** PUT /partners/{partnerId}/certificates/{certificateId}/rotate */
  async rotateCertificate(partnerId: number, certificateId: string) {
    const response = await api.put<Certificate>(`/partners/${partnerId}/certificates/${certificateId}/rotate`);
    return response.data;
  },

  /** PUT /partners/{partnerId}/certificates/rotate-all */
  async rotateAllCertificates(partnerId: number) {
    const response = await api.put<Certificate[]>(`/partners/${partnerId}/certificates/rotate-all`);
    return response.data;
  },

  /** DELETE /partners/{partnerId}/certificates/{certificateId} */
  async deleteCertificate(partnerId: number, certificateId: string) {
    await api.delete(`/partners/${partnerId}/certificates/${certificateId}`);
  },

  /** PUT /partners/{partnerId}/certificates/{certificateId}/deactivate */
  async deactivateCertificate(partnerId: number, certificateId: string) {
    const response = await api.put<Certificate>(`/partners/${partnerId}/certificates/${certificateId}/deactivate`);
    return response.data;
  },

  // === SNAP-BI Integration ===
  // BUG-FE-033: getSnapBiToken via client credentials removed from frontend.

  /** POST /partner/payments — Create SNAP-BI payment */
  async createSnapBiPayment(data: { amount: number; currency: string; referenceId: string; description?: string }) {
    const response = await api.post<SnapBiPayment>('/partner/payments', data);
    return response.data;
  },

  /** GET /partner/payments/{id} — Get SNAP-BI payment status */
  async getSnapBiPayment(id: string) {
    const response = await api.get<SnapBiPayment>(`/partner/payments/${id}`);
    return response.data;
  },

  /** POST /partner/payments/{id}/refund — Refund SNAP-BI payment */
  async refundSnapBiPayment(id: string) {
    const response = await api.post<SnapBiPayment>(`/partner/payments/${id}/refund`);
    return response.data;
  },
};
