import { apiClient } from './api';
import { FeedbackData, ApiResponse } from '@/types';

export const feedbackService = {
  async submitFeedback(feedback: FeedbackData): Promise<void> {
    await apiClient.post<ApiResponse<void>>('/feedback', feedback);
  },

  async uploadScreenshot(uri: string): Promise<string> {
    const formData = new FormData();
    formData.append('file', {
      uri,
      type: 'image/jpeg',
      name: 'screenshot.jpg',
    } as any);

    const response = await apiClient.post<ApiResponse<{ url: string }>>(
      '/feedback/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );

    return response.data.data.url;
  },
};
