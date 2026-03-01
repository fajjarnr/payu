import { AxiosRequestConfig } from 'axios';
import * as CryptoJS from 'crypto-js';

/**
 * Configuration for AuthInterceptor.
 */
export interface AuthConfig {
  apiKey: string;
  apiSecret: string;
}

/**
 * Axios request interceptor that adds authentication headers.
 *
 * Adds:
 * - X-API-Key: The API key
 * - X-Timestamp: Current timestamp
 * - X-Signature: HMAC-SHA256 signature of the request
 */
export class AuthInterceptor {
  private readonly config: AuthConfig;

  constructor(config: AuthConfig) {
    this.config = config;
  }

  /**
   * Intercept and modify the request to add auth headers.
   */
  intercept(requestConfig: AxiosRequestConfig): AxiosRequestConfig {
    const timestamp = Date.now().toString();
    const signature = this.generateSignature(requestConfig, timestamp);

    requestConfig.headers = {
      ...requestConfig.headers,
      'X-API-Key': this.config.apiKey,
      'X-Timestamp': timestamp,
      'X-Signature': signature
    };

    return requestConfig;
  }

  /**
   * Generate HMAC-SHA256 signature for the request.
   */
  private generateSignature(requestConfig: AxiosRequestConfig, timestamp: string): string {
    const method = (requestConfig.method || 'GET').toUpperCase();
    const path = requestConfig.url || '/';
    const body = requestConfig.data ? JSON.stringify(requestConfig.data) : '';

    // Signature format: METHOD|PATH|TIMESTAMP|BODY_HASH
    const stringToSign = `${method}|${path}|${timestamp}|${this.hashBody(body)}`;

    return CryptoJS.HmacSHA256(stringToSign, this.config.apiSecret).toString();
  }

  /**
   * Hash request body for signature.
   */
  private hashBody(body: string): string {
    if (!body) return '';
    return CryptoJS.SHA256(body).toString();
  }
}
