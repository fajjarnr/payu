/**
 * Sanitized logger. Security policy P2-C3: masks bearer/JWT tokens, NIK, card
 * numbers, PINs, passwords, API keys, and session IDs before logging.
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogEntry {
  level: LogLevel;
  message: string;
  timestamp: string;
  context?: Record<string, unknown>;
}

/** Masks sensitive patterns (bearer/JWT, NIK, PAN, PIN, password) in a value. */
function sanitizeValue(value: unknown, depth = 0): unknown {
  // Prevent infinite recursion
  if (depth > 10) {
    return '[Max depth reached]';
  }

  if (value === null || value === undefined) {
    return value;
  }

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

  if (Array.isArray(value)) {
    return value.map((item) => sanitizeValue(item, depth + 1));
  }

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
      sanitized[key] = sanitizeValue(val, depth + 1);
    }
  }

  return sanitized;
}

/** Sanitizes an error so tokens and auth headers are never logged. */
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

function formatLogEntry(entry: LogEntry): string {
  const { level, message, timestamp } = entry;
  const prefix = `[${timestamp}] [${level.toUpperCase()}]`;

  if (entry.context && Object.keys(entry.context).length > 0) {
    const sanitizedContext = sanitizeValue(entry.context);
    return `${prefix} ${message}\n${JSON.stringify(sanitizedContext, null, 2)}`;
  }

  return `${prefix} ${message}`;
}

function log(level: LogLevel, message: string, context?: Record<string, unknown>): void {
  const entry: LogEntry = {
    level,
    message,
    timestamp: new Date().toISOString(),
    context,
  };

  const formattedLog = formatLogEntry(entry);

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

/** Logger API; all methods sanitize sensitive data before logging. */
export const logger = {
  debug: (message: string, context?: Record<string, unknown>): void => {
    log('debug', message, context);
  },

  info: (message: string, context?: Record<string, unknown>): void => {
    log('info', message, context);
  },

  warn: (message: string, context?: Record<string, unknown>): void => {
    log('warn', message, context);
  },

  error: (message: string, error?: unknown, context?: Record<string, unknown>): void => {
    const errorContext = error ? sanitizeError(error) : undefined;
    const mergedContext = errorContext
      ? { ...context, ...errorContext }
      : context;

    log('error', message, mergedContext);
  },

  /** Creates a logger with predefined context. */
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

export function testLoggerSanitization(): void {
  if (!__DEV__) {
    return;
  }

  console.log('=== Testing Logger Sanitization ===');

  logger.info('JWT Token Test', {
    token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U',
  });

  logger.info('NIK Test', {
    nik: '3201010101010001',
    user: { name: 'John Doe', nik: '3201010101010001' },
  });

  logger.info('Card Test', {
    cardNumber: '4111111111111111',
    cvv: '123',
  });

  logger.info('Password Test', {
    password: 'secret123',
    newPassword: 'newPass456',
  });

  logger.info('Auth Header Test', {
    headers: {
      Authorization: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
    },
  });

  console.log('=== End Sanitization Test ===');
}

/** Class-based logger with category prefixes for API, retry, and idempotency logs. */
export class Logger {
  static debug(category: string, message: string, context?: Record<string, unknown>): void {
    logger.debug(`[${category}] ${message}`, context);
  }

  static info(category: string, message: string, context?: Record<string, unknown>): void {
    logger.info(`[${category}] ${message}`, context);
  }

  static warn(category: string, message: string, context?: Record<string, unknown>): void {
    logger.warn(`[${category}] ${message}`, context);
  }

  static error(
    category: string,
    message: string,
    error?: unknown,
    context?: Record<string, unknown>
  ): void {
    logger.error(`[${category}] ${message}`, error, context);
  }

  static apiRequest(method: string, url: string, context?: Record<string, unknown>): void {
    Logger.debug('API', `${method} ${url}`, context);
  }

  static apiResponse(
    method: string,
    url: string,
    status: number,
    duration: number,
    context?: Record<string, unknown>
  ): void {
    Logger.debug('API', `${method} ${url} - ${status} (${duration}ms)`, context);
  }

  static apiError(
    method: string,
    url: string,
    error: unknown,
    context?: Record<string, unknown> & { retryCount?: number }
  ): void {
    const retryInfo = context?.retryCount !== undefined ? ` (Retry: ${context.retryCount})` : '';
    Logger.error('API', `${method} ${url} failed${retryInfo}`, error, context);
  }

  static retry(operation: string, attempt: number, maxAttempts: number, delay: number): void {
    Logger.warn('Retry', `${operation} - Attempt ${attempt}/${maxAttempts} (retry in ${delay}ms)`);
  }

  static idempotency(operation: string, idempotencyKey: string, context?: Record<string, unknown>): void {
    // Mask the UUID part of the idempotency key
    const parts = idempotencyKey.split('::');
    const maskedKey = parts.length >= 2 ? `${parts[0]}::${parts[1] || ''}::***` : '***';

    Logger.debug('Idempotency', `${operation} - Key: ${maskedKey}`, context);
  }
}

export default logger;
