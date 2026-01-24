import { apiClient } from './api';
import { VirtualCard, ApiResponse } from '@/types';

export const cardService = {
  async getCards(): Promise<VirtualCard[]> {
    const response = await apiClient.get<ApiResponse<VirtualCard[]>>('/cards');
    return response.data.data;
  },

  async getCard(cardId: string): Promise<VirtualCard> {
    const response = await apiClient.get<ApiResponse<VirtualCard>>(`/cards/${cardId}`);
    return response.data.data;
  },

  async createCard(): Promise<VirtualCard> {
    const response = await apiClient.post<ApiResponse<VirtualCard>>('/cards');
    return response.data.data;
  },

  async freezeCard(cardId: string): Promise<void> {
    await apiClient.post(`/cards/${cardId}/freeze`);
  },

  async unfreezeCard(cardId: string): Promise<void> {
    await apiClient.post(`/cards/${cardId}/unfreeze`);
  },

  async setSpendingLimit(cardId: string, limit: number): Promise<void> {
    await apiClient.put(`/cards/${cardId}/limit`, { limit });
  },

  async cancelCard(cardId: string): Promise<void> {
    await apiClient.delete(`/cards/${cardId}`);
  },

  async getCardDetails(cardId: string): Promise<{
    cvv: string;
    cardNumber: string;
    expiryDate: string;
  }> {
    const response = await apiClient.get<ApiResponse<any>>(`/cards/${cardId}/details`);
    return response.data.data;
  },
};
