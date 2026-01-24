import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { API_CONFIG, AUTH_CONFIG } from '@/constants/config';
import { storage } from '@/utils/storage';
import { AuthTokens } from '@/types';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_CONFIG.BASE_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor
    this.client.interceptors.request.use(
      async (config: InternalAxiosRequestConfig) => {
        const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);

        if (tokens?.accessToken) {
          config.headers.Authorization = `Bearer ${tokens.accessToken}`;
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & {
          _retry?: boolean;
        };

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const tokens = await storage.get<AuthTokens>(AUTH_CONFIG.TOKEN_KEY);

            if (tokens?.refreshToken) {
              const response = await this.refreshToken(tokens.refreshToken);
              const newTokens = response.data;

              await storage.set(AUTH_CONFIG.TOKEN_KEY, newTokens);

              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
              }

              return this.client(originalRequest);
            }
          } catch (refreshError) {
            // Refresh failed, logout user
            await storage.remove(AUTH_CONFIG.TOKEN_KEY);
            await storage.remove(AUTH_CONFIG.USER_KEY);
            // Navigate to login (handled by auth context)
          }
        }

        return Promise.reject(error);
      }
    );
  }

  private async refreshToken(refreshToken: string) {
    return axios.post(`${API_CONFIG.BASE_URL}/auth/refresh`, {
      refreshToken,
    });
  }

  public getInstance() {
    return this.client;
  }
}

export const apiClient = new ApiClient().getInstance();
