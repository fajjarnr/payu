/**
 * Base error class for PayU SDK errors.
 */
export class PayUError extends Error {
  /** Error code for programmatic handling */
  public readonly code: string;
  /** HTTP status code (if applicable) */
  public readonly statusCode?: number;
  /** Original error that caused this error */
  public readonly cause?: Error;
  /** Additional error details */
  public readonly details?: Record<string, any>;

  constructor(
    code: string,
    message: string,
    cause?: Error,
    statusCode?: number,
    details?: Record<string, any>
  ) {
    super(message);
    this.name = 'PayUError';
    this.code = code;
    this.cause = cause;
    this.statusCode = statusCode;
    this.details = details;

    // Fix prototype chain for instanceof checks
    Object.setPrototypeOf(this, PayUError.prototype);
  }

  /**
   * Create a PayUError from an API error response.
   */
  static fromApiResponse(status: number, data: any, originalError: Error): PayUError {
    const errorCode = data?.errorCode || data?.code || `HTTP_${status}`;
    const message = data?.message || data?.error || `API Error: ${status}`;

    switch (status) {
      case 401:
        return new PayUAuthError(message, errorCode, originalError);
      case 403:
        return new PayUAuthError(message, errorCode, originalError, status);
      case 422:
        return new PayUValidationError(message, data?.errors, errorCode, originalError);
      case 429:
        return new PayUError('RATE_LIMITED', message, originalError, status, data);
      case 500:
      case 502:
      case 503:
      case 504:
        return new PayUApiError(message, status, errorCode, true, originalError);
      default:
        return new PayUApiError(message, status, errorCode, false, originalError);
    }
  }
}

/**
 * Error for API-related issues (non-2xx responses).
 */
export class PayUApiError extends PayUError {
  /** Whether the error is retryable */
  public readonly retryable: boolean;

  constructor(
    message: string,
    statusCode: number,
    code: string = 'API_ERROR',
    retryable: boolean = false,
    cause?: Error
  ) {
    super(code, message, cause, statusCode);
    this.name = 'PayUApiError';
    this.retryable = retryable;

    Object.setPrototypeOf(this, PayUApiError.prototype);
  }
}

/**
 * Error for authentication/authorization failures.
 */
export class PayUAuthError extends PayUError {
  constructor(
    message: string = 'Authentication failed',
    code: string = 'AUTH_ERROR',
    cause?: Error,
    statusCode: number = 401
  ) {
    super(code, message, cause, statusCode);
    this.name = 'PayUAuthError';

    Object.setPrototypeOf(this, PayUAuthError.prototype);
  }
}

/**
 * Error for validation failures.
 */
export class PayUValidationError extends PayUError {
  /** Field-specific validation errors */
  public readonly fieldErrors?: Record<string, string[]>;

  constructor(
    message: string = 'Validation failed',
    fieldErrors?: Record<string, string[]>,
    code: string = 'VALIDATION_ERROR',
    cause?: Error
  ) {
    super(code, message, cause, 422, { fieldErrors });
    this.name = 'PayUValidationError';
    this.fieldErrors = fieldErrors;

    Object.setPrototypeOf(this, PayUValidationError.prototype);
  }
}
