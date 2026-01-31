/**
 * Secure Logger Utility for PayU Mobile App
 *
 * SECURITY POLICY: Log Sanitization (P2-C3)
 * ===========================================
 *
 * This logger provides sanitized logging that prevents sensitive data
 * from appearing in console output, crash reports, and remote logging services.
 *
 * Sensitive Data Patterns Masked:
 * - Bearer tokens (Authorization headers)
 * - Access/refresh tokens
 * - NIK (Indonesian ID number) - 16 digits
 * - Card numbers (PAN) - 13-19 digits
 * - PIN codes
 * - Passwords
 * - API keys
 * - Session IDs
 *
 * Usage:
 * ```tsx
 * import { logger } from '@/utils/logger';
 *
 * // Sanitized logging - tokens automatically masked
 * logger.info('User login successful', { userId: 'user-123' });
 * logger.error('API request failed', { error: err, url: '/api/transfer' });
 * logger.debug('Token refresh', { token: 'Bearer eyJhbGci...' }); // Token masked
 * ```
 *
 * @module logger
 * @version 1.0.0 - Security-hardened logging
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogEntry {
  level: LogLevel;
  message: string;
  timestamp: string;
  context?: Record<string, unknown>;
}

/**
 * Sanitizes a value by masking sensitive data
 *
 * Patterns Masked:
 * - Bearer tokens: "Bearer <token>" -> "Bearer ***MASKED***"
 * - JWT tokens: eyJ... -> ***MASKED_TOKEN***
 * - NIK (16 digits): 3201... -> 3201****1234
 * - Card numbers (13-19 digits): 4111... -> 4111****1234
 * - PIN codes (4-6 digits): 1234 -> ****
 * - Passwords in any field name containing 'password', 'secret', 'token'
 */
function sanitizeValue(value: unknown, depth = 0): unknown {
  // Prevent infinite recursion
  if (depth > 10) {
    return '[Max depth reached]';
  }

  // Handle null/undefined
  if (value === null || value === undefined) {
    return value;
  }

  // Handle primitives
  if (typeof value !== 'object') {
    const strValue = String(value);

    // Mask JWT tokens (eyJhbGci...)
    if (/^eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(strValue)) {
      return '***MASKED_TOKEN***';
    }

    // Mask Bearer tokens
    if (strValue.toLowerCase().startsWith('bearer ')) {
      return 'Bearer ***MASKED***';
    }

    // Mask generic access/refresh tokens (long alphanumeric strings)
    if (/^[A-Za-z0-9_-]{32,}$/.test(strValue)) {
      return '***MASKED_TOKEN***';
    }

    // Mask NIK (16 digits - Indonesian ID)
    if (/^\d{16}$/.test(strValue)) {
      return `${strValue.slice(0, 4)}****${strValue.slice(-4)}`;
    }

    // Mask card numbers (13-19 digits)
    if (/^\d{13,19}$/.test(strValue)) {
      return `${strValue.slice(0, 4)}****${strValue.slice(-4)}`;
    }

    // Mask PIN codes (4-6 digits)
    if (/^\d{4,6}$/.test(strValue)) {
      return '****';
    }

    return value;
  }

  // Handle arrays
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeValue(item, depth + 1));
  }

  // Handle objects
  const sanitized: Record<string, unknown> = {};
  const obj = value as Record<string, unknown>;

  for (const [key, val] of Object.entries(obj)) {
    const lowerKey = key.toLowerCase();

    // Skip keys that are known to contain sensitive data
    if (
      lowerKey.includes('token') ||
      lowerKey.includes('password') ||
      lowerKey.includes('secret') ||
      lowerKey.includes('pin') ||
      lowerKey.includes('card') ||
      lowerKey.includes('nik') ||
      lowerKey.includes('ktp') ||
      lowerKey.includes('authorization') ||
      lowerKey.includes('apikey') ||
      lowerKey.includes('api_key') ||
      lowerKey.includes('session') ||
      lowerKey.includes('credential')
    ) {
      // Check if it's a string that might need partial masking (like card numbers)
      if (typeof val === 'string') {
        // Card number style: show first 4 and last 4
        if (/^\d{13,19}$/.test(val)) {
          sanitized[key] = `${val.slice(0, 4)}****${val.slice(-4)}`;
        }
        // NIK style: show first 4 and last 4
        else if (/^\d{16}$/.test(val)) {
          sanitized[key] = `${val.slice(0, 4)}****${val.slice(-4)}`;
        }
        // Complete mask for tokens/passwords
        else {
          sanitized[key] = '***MASKED***';
        }
      } else {
        sanitized[key] = '***MASKED***';
      }
    } else {
      // Recursively sanitize nested values
      sanitized[key] = sanitizeValue(val, depth + 1);
    }
  }

  return sanitized;
}

/**
 * Sanitizes an error object to prevent sensitive data leakage
 *
 * Error objects may contain:
 * - Request/response data with tokens
 * - Headers with authorization
 * - Stack traces with sensitive values
 */
function sanitizeError(error: unknown): Record<string, unknown> {
  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message,
      // Don't include stack trace in production logs
      stack: __DEV__ ? error.stack : '[Stack trace omitted]',
    };
  }

  if (typeof error === 'object' && error !== null) {
    const sanitized = sanitizeValue(error);
    return sanitized as Record<string, unknown>;
  }

  return { error: String(error) };
}

/**
 * Formats log entry with timestamp and level prefix
 */
function formatLogEntry(entry: LogEntry): string {
  const { level, message, timestamp } = entry;
  const prefix = `[${timestamp}] [${level.toUpperCase()}]`;

  if (entry.context && Object.keys(entry.context).length > 0) {
    const sanitizedContext = sanitizeValue(entry.context);
    return `${prefix} ${message}\n${JSON.stringify(sanitizedContext, null, 2)}`;
  }

  return `${prefix} ${message}`;
}

/**
 * Core logging function
 */
function log(level: LogLevel, message: string, context?: Record<string, unknown>): void {
  const entry: LogEntry = {
    level,
    message,
    timestamp: new Date().toISOString(),
    context,
  };

  const formattedLog = formatLogEntry(entry);

  // In production, you might want to send logs to a remote service
  // For now, we use console methods with appropriate severity
  switch (level) {
    case 'debug':
      if (__DEV__) {
        console.debug(formattedLog);
      }
      break;
    case 'info':
      console.info(formattedLog);
      break;
    case 'warn':
      console.warn(formattedLog);
      break;
    case 'error':
      console.error(formattedLog);
      break;
  }
}

/**
 * Secure Logger API
 *
 * All methods automatically sanitize sensitive data before logging.
 */
export const logger = {
  /**
   * Log debug message (development only)
   */
  debug: (message: string, context?: Record<string, unknown>): void => {
    log('debug', message, context);
  },

  /**
   * Log info message
   */
  info: (message: string, context?: Record<string, unknown>): void => {
    log('info', message, context);
  },

  /**
   * Log warning message
   */
  warn: (message: string, context?: Record<string, unknown>): void => {
    log('warn', message, context);
  },

  /**
   * Log error message with sanitized error details
   */
  error: (message: string, error?: unknown, context?: Record<string, unknown>): void => {
    const errorContext = error ? sanitizeError(error) : undefined;
    const mergedContext = errorContext
      ? { ...context, ...errorContext }
      : context;

    log('error', message, mergedContext);
  },

  /**
   * Create a scoped logger with predefined context
   *
   * @example
   * ```tsx
   * const authLogger = logger.scope({ module: 'auth' });
   * authLogger.info('User logged in', { userId: '123' });
   * // Output: [timestamp] [INFO] User logged in
   * //         {"module":"auth","userId":"123"}
   * ```
   */
  scope: (prefixContext: Record<string, unknown>) => {
    return {
      debug: (message: string, context?: Record<string, unknown>): void => {
        log('debug', message, { ...prefixContext, ...context });
      },
      info: (message: string, context?: Record<string, unknown>): void => {
        log('info', message, { ...prefixContext, ...context });
      },
      warn: (message: string, context?: Record<string, unknown>): void => {
        log('warn', message, { ...prefixContext, ...context });
      },
      error: (message: string, error?: unknown, context?: Record<string, unknown>): void => {
        const errorContext = error ? sanitizeError(error) : undefined;
        const mergedContext = errorContext
          ? { ...prefixContext, ...context, ...errorContext }
          : { ...prefixContext, ...context };
        log('error', message, mergedContext);
      },
    };
  },
};

/**
 * Development-only utility to test logger sanitization
 *
 * @example
 * ```tsx
 * if (__DEV__) {
 *   testLoggerSanitization();
 * }
 * ```
 */
export function testLoggerSanitization(): void {
  if (!__DEV__) {
    return;
  }

  console.log('=== Testing Logger Sanitization ===');

  // Test token masking
  logger.info('JWT Token Test', {
    token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U',
  });

  // Test NIK masking
  logger.info('NIK Test', {
    nik: '3201010101010001',
    user: { name: 'John Doe', nik: '3201010101010001' },
  });

  // Test card masking
  logger.info('Card Test', {
    cardNumber: '4111111111111111',
    cvv: '123',
  });

  // Test password masking
  logger.info('Password Test', {
    password: 'secret123',
    newPassword: 'newPass456',
  });

  // Test Bearer token masking
  logger.info('Auth Header Test', {
    headers: {
      Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
    },
  });

  console.log('=== End Sanitization Test ===');
}

/**
 * Class-based Logger API for structured logging with categories
 *
 * Provides additional methods for specific logging scenarios like API calls,
 * retries, and idempotency tracking.
 *
 * @example
 * ```tsx
 * import { Logger } from '@/utils/logger';
 *
 * Logger.debug('API', 'Request started', { url: '/api/transfer' });
 * Logger.apiResponse('GET', '/api/wallet', 200, 150);
 * Logger.apiError('POST', '/api/transfer', error, { retryCount: 1 });
 * ```
 */
export class Logger {
  /**
   * Log a debug message with category
   */
  static debug(category: string, message: string, context?: Record<string, unknown>): void {
    logger.debug(`[${category}] ${message}`, context);
  }

  /**
   * Log an info message with category
   */
  static info(category: string, message: string, context?: Record<string, unknown>): void {
    logger.info(`[${category}] ${message}`, context);
  }

  /**
   * Log a warning message with category
   */
  static warn(category: string, message: string, context?: Record<string, unknown>): void {
    logger.warn(`[${category}] ${message}`, context);
  }

  /**
   * Log an error message with category
   */
  static error(
    category: string,
    message: string,
    error?: unknown,
    context?: Record<string, unknown>
  ): void {
    logger.error(`[${category}] ${message}`, error, context);
  }

  /**
   * Log an API request
   */
  static apiRequest(method: string, url: string, context?: Record<string, unknown>): void {
    Logger.debug('API', `${method} ${url}`, context);
  }

  /**
   * Log an API response
   */
  static apiResponse(
    method: string,
    url: string,
    status: number,
    duration: number,
    context?: Record<string, unknown>
  ): void {
    Logger.debug('API', `${method} ${url} - ${status} (${duration}ms)`, context);
  }

  /**
   * Log an API error
   */
  static apiError(
    method: string,
    url: string,
    error: unknown,
    context?: Record<string, unknown> & { retryCount?: number }
  ): void {
    const retryInfo = context?.retryCount !== undefined ? ` (Retry: ${context.retryCount})` : '';
    Logger.error('API', `${method} ${url} failed${retryInfo}`, error, context);
  }

  /**
   * Log a retry operation
   */
  static retry(operation: string, attempt: number, maxAttempts: number, delay: number): void {
    Logger.warn('Retry', `${operation} - Attempt ${attempt}/${maxAttempts} (retry in ${delay}ms)`);
  }

  /**
   * Log idempotency key usage (with masking)
   */
  static idempotency(operation: string, idempotencyKey: string, context?: Record<string, unknown>): void {
    // Mask the UUID part of the idempotency key
    const parts = idempotencyKey.split('::');
    const maskedKey = parts.length >= 2 ? `${parts[0]}::${parts[1] || ''}::***` : '***';

    Logger.debug('Idempotency', `${operation} - Key: ${maskedKey}`, context);
  }
}

export default logger;
