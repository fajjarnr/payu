import api from '@/lib/api';
import type { User } from '@/types';

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  address?: string;
}

export class UserService {
  private static instance: UserService;

  private constructor() {}

  static getInstance(): UserService {
    if (!UserService.instance) {
      UserService.instance = new UserService();
    }
    return UserService.instance;
  }

  /**
   * Update user profile
   * PUT /api/v1/accounts/users/{userId}
   */
  async updateUser(userId: string, data: UpdateUserRequest): Promise<User> {
    const response = await api.put(`/accounts/users/${userId}`, data);
    return response.data;
  }

  /**
   * Get user profile
   * GET /api/v1/accounts/users/{userId}
   */
  async getUser(userId: string): Promise<User> {
    const response = await api.get(`/accounts/users/${userId}`);
    return response.data;
  }
}

export default UserService.getInstance();
