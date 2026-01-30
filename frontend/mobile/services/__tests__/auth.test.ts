import { authService } from '../auth.service';
import { apiClient } from '../api';
import { LoginCredentials, RegisterData, AuthResponse, ApiResponse } from '@/types';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    post: jest.fn(),
  },
}));

describe('authService', () => {
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('login', () => {
    const credentials: LoginCredentials = {
      identifier: 'user@example.com',
      password: 'password123',
    };

    const mockAuthResponse: AuthResponse = {
      user: {
        id: 'user-123',
        email: 'user@example.com',
        phoneNumber: '+6281234567890',
        fullName: 'Test User',
        kycVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
      },
      tokens: {
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-123',
        expiresIn: 3600,
      },
    };

    it('should login successfully with valid credentials', async () => {
      const apiResponse: ApiResponse<AuthResponse> = {
        success: true,
        data: mockAuthResponse,
        message: 'Login successful',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await authService.login(credentials);

      expect(mockPost).toHaveBeenCalledWith('/auth/login', credentials);
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockAuthResponse);
    });

    it('should handle login failure with invalid credentials', async () => {
      const error = new Error('Invalid credentials');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.login(credentials)).rejects.toThrow('Invalid credentials');
      expect(mockPost).toHaveBeenCalledWith('/auth/login', credentials);
    });

    it('should handle network errors during login', async () => {
      const error = new Error('Network Error');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.login(credentials)).rejects.toThrow('Network Error');
    });

    it('should handle server errors (500) during login', async () => {
      const error = new Error('Internal Server Error');
      (error as any).response = { status: 500 };
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.login(credentials)).rejects.toThrow('Internal Server Error');
    });
  });

  describe('register', () => {
    const registerData: RegisterData = {
      email: 'newuser@example.com',
      phoneNumber: '+6281234567890',
      fullName: 'New User',
      password: 'password123',
      confirmPassword: 'password123',
    };

    const mockAuthResponse: AuthResponse = {
      user: {
        id: 'user-456',
        email: 'newuser@example.com',
        phoneNumber: '+6281234567890',
        fullName: 'New User',
        kycVerified: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
      tokens: {
        accessToken: 'access-token-456',
        refreshToken: 'refresh-token-456',
        expiresIn: 3600,
      },
    };

    it('should register successfully with valid data', async () => {
      const apiResponse: ApiResponse<AuthResponse> = {
        success: true,
        data: mockAuthResponse,
        message: 'Registration successful',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await authService.register(registerData);

      expect(mockPost).toHaveBeenCalledWith('/auth/register', registerData);
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockAuthResponse);
    });

    it('should handle registration failure with duplicate email', async () => {
      const error = new Error('Email already registered');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.register(registerData)).rejects.toThrow('Email already registered');
    });

    it('should handle registration failure with invalid data', async () => {
      const error = new Error('Validation failed');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.register(registerData)).rejects.toThrow('Validation failed');
    });
  });

  describe('logout', () => {
    it('should logout successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.logout();

      expect(mockPost).toHaveBeenCalledWith('/auth/logout');
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle logout failure gracefully', async () => {
      const error = new Error('Session expired');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.logout()).rejects.toThrow('Session expired');
    });

    it('should handle network error during logout', async () => {
      const error = new Error('Network Error');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.logout()).rejects.toThrow('Network Error');
    });
  });

  describe('refreshToken', () => {
    const refreshToken = 'old-refresh-token';

    const mockAuthResponse: AuthResponse = {
      user: {
        id: 'user-123',
        email: 'user@example.com',
        phoneNumber: '+6281234567890',
        fullName: 'Test User',
        kycVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
      },
      tokens: {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600,
      },
    };

    it('should refresh token successfully', async () => {
      const apiResponse: ApiResponse<AuthResponse> = {
        success: true,
        data: mockAuthResponse,
        message: 'Token refreshed',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await authService.refreshToken(refreshToken);

      expect(mockPost).toHaveBeenCalledWith('/auth/refresh', { refreshToken });
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockAuthResponse);
    });

    it('should handle refresh token failure', async () => {
      const error = new Error('Invalid refresh token');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.refreshToken(refreshToken)).rejects.toThrow('Invalid refresh token');
    });

    it('should handle expired refresh token', async () => {
      const error = new Error('Refresh token expired');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.refreshToken(refreshToken)).rejects.toThrow('Refresh token expired');
    });
  });

  describe('verifyEmail', () => {
    const token = 'email-verification-token';

    it('should verify email successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.verifyEmail(token);

      expect(mockPost).toHaveBeenCalledWith('/auth/verify-email', { token });
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle invalid verification token', async () => {
      const error = new Error('Invalid or expired token');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.verifyEmail(token)).rejects.toThrow('Invalid or expired token');
    });
  });

  describe('requestPasswordReset', () => {
    const email = 'user@example.com';

    it('should request password reset successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.requestPasswordReset(email);

      expect(mockPost).toHaveBeenCalledWith('/auth/forgot-password', { email });
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle non-existent email gracefully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.requestPasswordReset('nonexistent@example.com');

      expect(mockPost).toHaveBeenCalledWith('/auth/forgot-password', { email: 'nonexistent@example.com' });
    });

    it('should handle server error during password reset request', async () => {
      const error = new Error('Server Error');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.requestPasswordReset(email)).rejects.toThrow('Server Error');
    });
  });

  describe('resetPassword', () => {
    const token = 'reset-token';
    const password = 'newpassword123';

    it('should reset password successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.resetPassword(token, password);

      expect(mockPost).toHaveBeenCalledWith('/auth/reset-password', { token, password });
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle invalid reset token', async () => {
      const error = new Error('Invalid or expired token');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.resetPassword(token, password)).rejects.toThrow('Invalid or expired token');
    });

    it('should handle weak password error', async () => {
      const error = new Error('Password does not meet requirements');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.resetPassword(token, 'weak')).rejects.toThrow('Password does not meet requirements');
    });
  });

  describe('changePassword', () => {
    const oldPassword = 'oldpassword123';
    const newPassword = 'newpassword123';

    it('should change password successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await authService.changePassword(oldPassword, newPassword);

      expect(mockPost).toHaveBeenCalledWith('/auth/change-password', { oldPassword, newPassword });
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle incorrect old password', async () => {
      const error = new Error('Current password is incorrect');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.changePassword(oldPassword, newPassword)).rejects.toThrow('Current password is incorrect');
    });

    it('should handle same password error', async () => {
      const error = new Error('New password must be different from old password');
      mockPost.mockRejectedValueOnce(error);

      await expect(authService.changePassword('samepassword', 'samepassword')).rejects.toThrow('New password must be different from old password');
    });
  });
});
