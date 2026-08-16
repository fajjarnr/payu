import { AxiosInstance, AxiosRequestConfig } from 'axios';
import {
  ApiResponse,
  BalanceResponse,
  CreatePaymentRequest,
  InitiateTransferRequest,
  PaymentResponse,
  TransactionResponse
} from './models';

interface ApiConfiguration {
  basePath?: string;
}

/**
 * Base API client that all generated resource clients share.
 */
abstract class BaseApi {
  protected readonly httpClient: AxiosInstance;
  protected readonly baseUrl: string;

  constructor(configuration?: ApiConfiguration, basePath?: string, httpClient?: AxiosInstance) {
    this.baseUrl = basePath || '';
    this.httpClient = httpClient as AxiosInstance;
    if (!this.httpClient) {
      throw new Error('PayUClient requires an Axios instance');
    }
  }

  protected async request<T>(config: AxiosRequestConfig): Promise<T> {
    const response = await this.httpClient.request<T>({
      baseURL: this.baseUrl,
      ...config
    });
    return response.data;
  }
}

/**
 * Payments API (bill payments).
 */
export class PaymentsApi extends BaseApi {
  /**
   * Create a bill payment.
   */
  create(body: CreatePaymentRequest, idempotencyKey?: string): Promise<ApiResponse<PaymentResponse>> {
    return this.request<ApiResponse<PaymentResponse>>({
      method: 'POST',
      url: '/api/v1/payments',
      data: body,
      headers: idempotencyKey ? { 'X-Idempotency-Key': idempotencyKey } : undefined
    });
  }

  /**
   * Get payment status by ID.
   */
  getStatus(id: string): Promise<ApiResponse<PaymentResponse>> {
    return this.request<ApiResponse<PaymentResponse>>({
      method: 'GET',
      url: `/api/v1/payments/${encodeURIComponent(id)}`
    });
  }
}

/**
 * Transfers API (internal transfers).
 */
export class TransfersApi extends BaseApi {
  /**
   * Initiate a transfer.
   */
  create(body: InitiateTransferRequest, idempotencyKey?: string): Promise<ApiResponse<TransactionResponse>> {
    return this.request<ApiResponse<TransactionResponse>>({
      method: 'POST',
      url: '/api/v1/transactions/transfer',
      data: body,
      headers: idempotencyKey ? { 'X-Idempotency-Key': idempotencyKey } : undefined
    });
  }

  /**
   * Get transfer status by ID.
   */
  getStatus(id: string): Promise<ApiResponse<TransactionResponse>> {
    return this.request<ApiResponse<TransactionResponse>>({
      method: 'GET',
      url: `/api/v1/transactions/${encodeURIComponent(id)}`
    });
  }
}

/**
 * Wallets API.
 */
export class WalletsApi extends BaseApi {
  /**
   * Get wallet balance for an account.
   */
  getBalance(accountId: string): Promise<ApiResponse<BalanceResponse>> {
    return this.request<ApiResponse<BalanceResponse>>({
      method: 'GET',
      url: `/api/v1/wallets/${encodeURIComponent(accountId)}/balance`
    });
  }
}

/**
 * Transactions API.
 */
export class TransactionsApi extends BaseApi {
  /**
   * Get transaction details by ID.
   */
  get(id: string): Promise<ApiResponse<TransactionResponse>> {
    return this.request<ApiResponse<TransactionResponse>>({
      method: 'GET',
      url: `/api/v1/transactions/${encodeURIComponent(id)}`
    });
  }
}
