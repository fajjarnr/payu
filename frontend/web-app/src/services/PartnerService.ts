import api from '@/lib/api';

export interface Partner {
  id: number;
  name: string;
  type: string;
  email: string;
  phone: string;
  clientId?: string;
  clientSecret?: string;
  publicKey?: string;
  active: boolean;
}

export const PartnerService = {
  async register(data: { name: string; email: string; type: string; phone: string; publicKey?: string }) {
    const response = await api.post<Partner>('/partners', data);
    return response.data;
  },

  async getProfile(id: number) {
    const response = await api.get<Partner>(`/partners/${id}`);
    return response.data;
  }
};
