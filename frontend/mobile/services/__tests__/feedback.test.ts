import { feedbackService } from '../feedback.service';
import { apiClient } from '../api';
import { FeedbackData, ApiResponse } from '@/types';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    post: jest.fn(),
  },
}));

describe('feedbackService', () => {
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('submitFeedback', () => {
    const mockFeedback: FeedbackData = {
      category: 'bug',
      rating: 3,
      message: 'App crashes when trying to transfer',
      screenshots: ['screenshot1.jpg', 'screenshot2.jpg'],
      deviceInfo: {
        appVersion: '1.0.0',
        os: 'iOS',
        osVersion: '17.0',
        device: 'iPhone 15 Pro',
      },
    };

    it('should submit feedback successfully', async () => {
      const apiResponse: ApiResponse<void> = {
        success: true,
        data: undefined,
        message: 'Feedback submitted successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.submitFeedback(mockFeedback);

      expect(mockPost).toHaveBeenCalledWith('/feedback', mockFeedback);
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should submit feedback without screenshots', async () => {
      const feedbackWithoutScreenshots = {
        ...mockFeedback,
        screenshots: undefined,
      };

      const apiResponse: ApiResponse<void> = {
        success: true,
        data: undefined,
        message: 'Feedback submitted successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.submitFeedback(feedbackWithoutScreenshots);

      expect(mockPost).toHaveBeenCalledWith('/feedback', feedbackWithoutScreenshots);
    });

    it('should submit feedback for different categories', async () => {
      const categories: FeedbackData['category'][] = ['bug', 'feature', 'ui', 'performance', 'other'];

      for (const category of categories) {
        jest.clearAllMocks();

        const feedback = { ...mockFeedback, category };
        const apiResponse: ApiResponse<void> = {
          success: true,
          data: undefined,
          message: 'Feedback submitted successfully',
        };

        mockPost.mockResolvedValueOnce({ data: apiResponse });

        await feedbackService.submitFeedback(feedback);

        expect(mockPost).toHaveBeenCalledWith('/feedback', feedback);
      }
    });

    it('should submit feedback with minimum rating', async () => {
      const feedbackWithMinRating = {
        ...mockFeedback,
        rating: 1,
      };

      const apiResponse: ApiResponse<void> = {
        success: true,
        data: undefined,
        message: 'Feedback submitted successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.submitFeedback(feedbackWithMinRating);

      expect(mockPost).toHaveBeenCalledWith('/feedback', feedbackWithMinRating);
    });

    it('should submit feedback with maximum rating', async () => {
      const feedbackWithMaxRating = {
        ...mockFeedback,
        rating: 5,
      };

      const apiResponse: ApiResponse<void> = {
        success: true,
        data: undefined,
        message: 'Feedback submitted successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.submitFeedback(feedbackWithMaxRating);

      expect(mockPost).toHaveBeenCalledWith('/feedback', feedbackWithMaxRating);
    });

    it('should handle network errors during submission', async () => {
      const error = new Error('Network Error');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.submitFeedback(mockFeedback))
        .rejects.toThrow('Network Error');
    });

    it('should handle server errors (500) during submission', async () => {
      const error = new Error('Internal Server Error');
      (error as any).response = { status: 500 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.submitFeedback(mockFeedback))
        .rejects.toThrow('Internal Server Error');
    });

    it('should handle validation errors (400)', async () => {
      const error = new Error('Validation failed: message is required');
      (error as any).response = { status: 400 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.submitFeedback(mockFeedback))
        .rejects.toThrow('Validation failed: message is required');
    });

    it('should handle unauthorized access (401)', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 401 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.submitFeedback(mockFeedback))
        .rejects.toThrow('Unauthorized');
    });

    it('should handle rate limiting (429)', async () => {
      const error = new Error('Too many requests');
      (error as any).response = { status: 429 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.submitFeedback(mockFeedback))
        .rejects.toThrow('Too many requests');
    });

    it('should handle empty message error', async () => {
      const error = new Error('Message cannot be empty');
      mockPost.mockRejectedValueOnce(error);

      const invalidFeedback = { ...mockFeedback, message: '' };
      await expect(feedbackService.submitFeedback(invalidFeedback))
        .rejects.toThrow('Message cannot be empty');
    });

    it('should handle invalid rating error', async () => {
      const error = new Error('Rating must be between 1 and 5');
      mockPost.mockRejectedValueOnce(error);

      const invalidFeedback = { ...mockFeedback, rating: 0 };
      await expect(feedbackService.submitFeedback(invalidFeedback))
        .rejects.toThrow('Rating must be between 1 and 5');
    });

    it('should handle missing device info error', async () => {
      const error = new Error('Device info is required');
      mockPost.mockRejectedValueOnce(error);

      const invalidFeedback = { ...mockFeedback, deviceInfo: undefined as any };
      await expect(feedbackService.submitFeedback(invalidFeedback))
        .rejects.toThrow('Device info is required');
    });

    it('should handle long message submission', async () => {
      const longFeedback = {
        ...mockFeedback,
        message: 'A'.repeat(5000),
      };

      const apiResponse: ApiResponse<void> = {
        success: true,
        data: undefined,
        message: 'Feedback submitted successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.submitFeedback(longFeedback);

      expect(mockPost).toHaveBeenCalledWith('/feedback', longFeedback);
    });
  });

  describe('uploadScreenshot', () => {
    const mockUri = 'file:///path/to/screenshot.jpg';
    const mockUploadResponse = { url: 'https://cdn.payu.com/screenshots/abc123.jpg' };

    it('should upload screenshot successfully', async () => {
      const apiResponse: ApiResponse<typeof mockUploadResponse> = {
        success: true,
        data: mockUploadResponse,
        message: 'Screenshot uploaded successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await feedbackService.uploadScreenshot(mockUri);

      expect(mockPost).toHaveBeenCalledWith(
        '/feedback/upload',
        expect.any(FormData),
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toBe(mockUploadResponse.url);
    });

    it('should handle file not found error', async () => {
      const error = new Error('File not found');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot('/invalid/path.jpg'))
        .rejects.toThrow('File not found');
    });

    it('should handle unsupported file format', async () => {
      const error = new Error('Unsupported file format');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot('file:///path/to/screenshot.png'))
        .rejects.toThrow('Unsupported file format');
    });

    it('should handle file too large error', async () => {
      const error = new Error('File size exceeds maximum limit');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot(mockUri))
        .rejects.toThrow('File size exceeds maximum limit');
    });

    it('should handle network error during upload', async () => {
      const error = new Error('Network Error');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot(mockUri))
        .rejects.toThrow('Network Error');
    });

    it('should handle server error during upload', async () => {
      const error = new Error('Upload failed');
      (error as any).response = { status: 500 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot(mockUri))
        .rejects.toThrow('Upload failed');
    });

    it('should handle unauthorized upload attempt', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 401 };
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot(mockUri))
        .rejects.toThrow('Unauthorized');
    });

    it('should handle empty URI', async () => {
      const error = new Error('Invalid file path');
      mockPost.mockRejectedValueOnce(error);

      await expect(feedbackService.uploadScreenshot(''))
        .rejects.toThrow('Invalid file path');
    });

    it('should set correct FormData structure', async () => {
      const apiResponse: ApiResponse<typeof mockUploadResponse> = {
        success: true,
        data: mockUploadResponse,
        message: 'Screenshot uploaded successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      await feedbackService.uploadScreenshot(mockUri);

      const formDataArg = mockPost.mock.calls[0][1] as FormData;
      expect(formDataArg).toBeInstanceOf(FormData);
    });
  });
});
