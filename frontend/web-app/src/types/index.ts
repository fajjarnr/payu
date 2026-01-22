import { z } from 'zod';

export const registerUserSchema = z.object({
  externalId: z.string().min(1, 'External ID is required'),
  username: z.string().min(3, 'Username must be at least 3 characters'),
  email: z.string().email('Invalid email address'),
  phoneNumber: z.string().min(10, 'Phone number is too short').optional(),
  fullName: z.string().min(1, 'Full name is required'),
  nik: z.string().length(16, 'NIK must be exactly 16 digits'),
});

export type RegisterUserRequest = z.infer<typeof registerUserSchema>;

export const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

export type LoginRequest = z.infer<typeof loginSchema>;

export interface User {
  id: string;
  externalId: string;
  username: string;
  email: string;
  fullName: string;
  kycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface Pocket {
  id: string;
  name: string;
  balance: number;
  target: number;
  type: 'MAIN' | 'SAVING' | 'SHARED';
}
