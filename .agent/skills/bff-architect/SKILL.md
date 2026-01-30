---
name: bff-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [api-architect]
tags: [backend, nodejs, bff, graphql, typescript, prisma]
related: [api-architect, frontend-architect, cybersecurity-architect]
description: **Master Skill**: BFF (Backend-for-Frontend) Architect. Expert in Node.js/TypeScript, Prisma ORM, Zod validation, Repository pattern, Caching, Error handling, and high-performance Express/Fastify patterns.
---

# PayU BFF Architect Master Skill

You are the **Lead Node.js Architect** for the **PayU Platform**. You design and build ultra-fast, type-safe services and BFFs that power our web and mobile applications using modern TypeScript patterns.

---

## 🏗️ Layered Architecture

### The Clean Node Pattern

```
┌─────────────────────────────────────────────────┐
│                   Routes                        │  ← Entry/Exit only
├─────────────────────────────────────────────────┤
│                 Controllers                     │  ← Coordinate requests
├─────────────────────────────────────────────────┤
│                  Services                       │  ← Business logic (DI)
├─────────────────────────────────────────────────┤
│                Repositories                     │  ← Data access (Prisma)
└─────────────────────────────────────────────────┘
```

### Repository Pattern

```typescript
// repositories/account.repository.ts
interface AccountRepository {
  findAll(filters?: AccountFilters): Promise<Account[]>;
  findById(id: string): Promise<Account | null>;
  create(data: CreateAccountDto): Promise<Account>;
  update(id: string, data: UpdateAccountDto): Promise<Account>;
  delete(id: string): Promise<void>;
}

class PrismaAccountRepository implements AccountRepository {
  constructor(private prisma: PrismaClient) {}

  async findAll(filters?: AccountFilters): Promise<Account[]> {
    return this.prisma.account.findMany({
      where: {
        ...(filters?.status && { status: filters.status }),
        ...(filters?.userId && { userId: filters.userId }),
      },
      take: filters?.limit ?? 100,
      orderBy: { createdAt: 'desc' },
    });
  }

  async findById(id: string): Promise<Account | null> {
    return this.prisma.account.findUnique({ where: { id } });
  }

  async create(data: CreateAccountDto): Promise<Account> {
    return this.prisma.account.create({ data });
  }

  async update(id: string, data: UpdateAccountDto): Promise<Account> {
    return this.prisma.account.update({ where: { id }, data });
  }

  async delete(id: string): Promise<void> {
    await this.prisma.account.delete({ where: { id } });
  }
}
```

### Service Layer Pattern

```typescript
// services/account.service.ts
class AccountService {
  constructor(
    private accountRepo: AccountRepository,
    private notificationService: NotificationService
  ) {}

  async getAccountWithBalance(accountId: string): Promise<AccountWithBalance> {
    const account = await this.accountRepo.findById(accountId);
    if (!account) throw new NotFoundError('Account not found');

    const balance = await this.calculateBalance(accountId);
    return { ...account, balance };
  }

  async transfer(from: string, to: string, amount: number): Promise<Transfer> {
    // Validate accounts
    const [sourceAccount, targetAccount] = await Promise.all([
      this.accountRepo.findById(from),
      this.accountRepo.findById(to),
    ]);

    if (!sourceAccount || !targetAccount) {
      throw new NotFoundError('Account not found');
    }

    // Check balance
    const balance = await this.calculateBalance(from);
    if (balance < amount) {
      throw new InsufficientFundsError('Insufficient balance');
    }

    // Execute transfer in transaction
    const transfer = await this.prisma.$transaction(async (tx) => {
      // Create transfer record and update balances
      return tx.transfer.create({
        data: { fromAccountId: from, toAccountId: to, amount },
      });
    });

    // Send notification (fire-and-forget)
    this.notificationService.sendTransferNotification(transfer);

    return transfer;
  }
}
```

---

## 🔐 Type-Safe Validation (Zod)

### Schema Definition

```typescript
// schemas/account.schema.ts
import { z } from 'zod';

export const createAccountSchema = z.object({
  body: z.object({
    name: z.string().min(1).max(100),
    type: z.enum(['SAVINGS', 'CHECKING', 'INVESTMENT']),
    currency: z.string().length(3).default('IDR'),
    initialDeposit: z.number().positive().optional(),
  }),
});

export const transferSchema = z.object({
  body: z.object({
    fromAccountId: z.string().uuid(),
    toAccountId: z.string().uuid(),
    amount: z.number().positive().max(100000000),
    description: z.string().max(200).optional(),
  }),
  headers: z.object({
    'idempotency-key': z.string().uuid(),
  }),
});

// Infer types from schema
export type CreateAccountDto = z.infer<typeof createAccountSchema>['body'];
export type TransferDto = z.infer<typeof transferSchema>['body'];
```

### Validation Middleware

```typescript
// middleware/validate.ts
import { AnyZodObject, ZodError } from 'zod';

export const validate = (schema: AnyZodObject) =>
  async (req: Request, res: Response, next: NextFunction) => {
    try {
      await schema.parseAsync({
        body: req.body,
        query: req.query,
        params: req.params,
        headers: req.headers,
      });
      return next();
    } catch (error) {
      if (error instanceof ZodError) {
        return res.status(400).json({
          success: false,
          error: 'Validation failed',
          details: error.errors.map(e => ({
            field: e.path.join('.'),
            message: e.message,
          })),
        });
      }
      return next(error);
    }
  };

// Usage
app.post('/api/accounts', validate(createAccountSchema), accountController.create);
```

---

## 🚀 Caching Strategies

### Redis Caching Layer

```typescript
// cache/cached-repository.ts
class CachedAccountRepository implements AccountRepository {
  constructor(
    private baseRepo: AccountRepository,
    private redis: Redis
  ) {}

  async findById(id: string): Promise<Account | null> {
    const cacheKey = `account:${id}`;

    // Try cache first
    const cached = await this.redis.get(cacheKey);
    if (cached) {
      return JSON.parse(cached);
    }

    // Cache miss - fetch from database
    const account = await this.baseRepo.findById(id);

    if (account) {
      await this.redis.setex(cacheKey, 300, JSON.stringify(account)); // 5 min TTL
    }

    return account;
  }

  async invalidateCache(id: string): Promise<void> {
    await this.redis.del(`account:${id}`);
  }

  // Invalidate on mutations
  async update(id: string, data: UpdateAccountDto): Promise<Account> {
    const result = await this.baseRepo.update(id, data);
    await this.invalidateCache(id);
    return result;
  }
}
```

### Cache-Aside Pattern

```typescript
// utils/cache.ts
async function getWithCache<T>(
  key: string,
  fetcher: () => Promise<T>,
  ttlSeconds: number = 300
): Promise<T> {
  const cached = await redis.get(key);
  if (cached) return JSON.parse(cached);

  const data = await fetcher();
  await redis.setex(key, ttlSeconds, JSON.stringify(data));

  return data;
}

// Usage
const account = await getWithCache(
  `account:${id}`,
  () => accountRepo.findById(id),
  300
);
```

---

## ⚠️ Error Handling

### Centralized Error Handler

```typescript
// errors/api-error.ts
export class ApiError extends Error {
  constructor(
    public statusCode: number,
    public message: string,
    public isOperational = true
  ) {
    super(message);
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}

export class NotFoundError extends ApiError {
  constructor(message = 'Resource not found') {
    super(404, message);
  }
}

export class InsufficientFundsError extends ApiError {
  constructor(message = 'Insufficient funds') {
    super(400, message);
  }
}

// middleware/error-handler.ts
export function errorHandler(
  error: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Log error
  logger.error('Request failed', error, {
    requestId: req.headers['x-request-id'],
    path: req.path,
    method: req.method,
  });

  if (error instanceof ApiError) {
    return res.status(error.statusCode).json({
      success: false,
      error: error.message,
    });
  }

  if (error instanceof ZodError) {
    return res.status(400).json({
      success: false,
      error: 'Validation failed',
      details: error.errors,
    });
  }

  // Unexpected error
  return res.status(500).json({
    success: false,
    error: 'Internal server error',
  });
}
```

### Retry with Exponential Backoff

```typescript
// utils/retry.ts
async function fetchWithRetry<T>(
  fn: () => Promise<T>,
  maxRetries = 3
): Promise<T> {
  let lastError: Error;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;

      if (i < maxRetries - 1) {
        const delay = Math.pow(2, i) * 1000; // 1s, 2s, 4s
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }

  throw lastError!;
}

// Usage - Calling external services
const exchangeRate = await fetchWithRetry(
  () => fxService.getRate('USD', 'IDR'),
  3
);
```

---

## 🔄 Background Jobs

### Simple Queue Pattern

```typescript
// queue/job-queue.ts
interface Job<T> {
  id: string;
  data: T;
  attempts: number;
  createdAt: Date;
}

class JobQueue<T> {
  private queue: Job<T>[] = [];
  private processing = false;

  async add(data: T): Promise<string> {
    const job: Job<T> = {
      id: crypto.randomUUID(),
      data,
      attempts: 0,
      createdAt: new Date(),
    };

    this.queue.push(job);

    if (!this.processing) {
      this.process();
    }

    return job.id;
  }

  private async process(): Promise<void> {
    this.processing = true;

    while (this.queue.length > 0) {
      const job = this.queue.shift()!;

      try {
        await this.execute(job);
      } catch (error) {
        job.attempts++;
        if (job.attempts < 3) {
          this.queue.push(job); // Retry
        } else {
          logger.error('Job failed permanently', error as Error, { jobId: job.id });
        }
      }
    }

    this.processing = false;
  }

  protected async execute(job: Job<T>): Promise<void> {
    // Override in subclass
  }
}

// Usage - Notification queue
class NotificationQueue extends JobQueue<NotificationPayload> {
  protected async execute(job: Job<NotificationPayload>): Promise<void> {
    await notificationService.send(job.data);
  }
}

const notificationQueue = new NotificationQueue();
await notificationQueue.add({ userId: '123', message: 'Transfer completed' });
```

---

## 📊 Structured Logging

```typescript
// logger/logger.ts
interface LogContext {
  userId?: string;
  requestId?: string;
  traceId?: string;
  method?: string;
  path?: string;
  [key: string]: unknown;
}

class Logger {
  private context: LogContext = {};

  withContext(ctx: LogContext): Logger {
    const logger = new Logger();
    logger.context = { ...this.context, ...ctx };
    return logger;
  }

  log(level: 'info' | 'warn' | 'error', message: string, extra?: LogContext) {
    const entry = {
      timestamp: new Date().toISOString(),
      level,
      message,
      ...this.context,
      ...extra,
    };

    console.log(JSON.stringify(entry));
  }

  info(message: string, extra?: LogContext) {
    this.log('info', message, extra);
  }

  warn(message: string, extra?: LogContext) {
    this.log('warn', message, extra);
  }

  error(message: string, error: Error, extra?: LogContext) {
    this.log('error', message, {
      ...extra,
      error: error.message,
      stack: error.stack,
    });
  }
}

export const logger = new Logger();

// Usage with context propagation
app.use((req, res, next) => {
  const requestId = req.headers['x-request-id'] || crypto.randomUUID();
  req.logger = logger.withContext({ requestId, method: req.method, path: req.path });
  next();
});
```

---

## ⚡ Prisma ORM Best Practices

### Connection Pooling

```typescript
// prisma/client.ts
import { PrismaClient } from '@prisma/client';

const globalForPrisma = globalThis as unknown as { prisma: PrismaClient };

export const prisma = globalForPrisma.prisma ?? new PrismaClient({
  log: process.env.NODE_ENV === 'development' 
    ? ['query', 'error', 'warn'] 
    : ['error'],
});

if (process.env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma;
}
```

### N+1 Query Prevention

```typescript
// ❌ BAD: N+1 queries
const accounts = await prisma.account.findMany();
for (const account of accounts) {
  account.owner = await prisma.user.findUnique({ where: { id: account.userId } });
}

// ✅ GOOD: Single query with include
const accounts = await prisma.account.findMany({
  include: { owner: true },
});

// ✅ GOOD: Batch fetch
const accounts = await prisma.account.findMany();
const userIds = accounts.map(a => a.userId);
const users = await prisma.user.findMany({ where: { id: { in: userIds } } });
const userMap = new Map(users.map(u => [u.id, u]));

accounts.forEach(account => {
  account.owner = userMap.get(account.userId);
});
```

### Transactions

```typescript
// Atomic transfer with transaction
async function executeTransfer(from: string, to: string, amount: number) {
  return prisma.$transaction(async (tx) => {
    // Debit source account
    await tx.account.update({
      where: { id: from },
      data: { balance: { decrement: amount } },
    });

    // Credit target account
    await tx.account.update({
      where: { id: to },
      data: { balance: { increment: amount } },
    });

    // Create transfer record
    return tx.transfer.create({
      data: { fromAccountId: from, toAccountId: to, amount },
    });
  });
}
```

---

## 🔄 Circuit Breaker (Opossum)

```typescript
// resilience/circuit-breaker.ts
import CircuitBreaker from 'opossum';

const options = {
  timeout: 3000,              // 3 seconds
  errorThresholdPercentage: 50,  // Open after 50% failures
  resetTimeout: 30000,        // Try again after 30 seconds
};

const fxServiceBreaker = new CircuitBreaker(
  async (from: string, to: string) => {
    const response = await fetch(`${FX_SERVICE_URL}/rates?from=${from}&to=${to}`);
    if (!response.ok) throw new Error('FX service unavailable');
    return response.json();
  },
  options
);

fxServiceBreaker.on('open', () => logger.warn('FX service circuit opened'));
fxServiceBreaker.on('close', () => logger.info('FX service circuit closed'));

// Usage
export async function getExchangeRate(from: string, to: string) {
  try {
    return await fxServiceBreaker.fire(from, to);
  } catch (error) {
    // Fallback to cached rate
    return getCachedRate(from, to);
  }
}
```

---

## � 7 Core Principles

### 1. Routes Only Route, Controllers Control

```typescript
// ❌ NEVER: Business logic in routes
router.post('/submit', async (req, res) => {
    // 200 lines of logic...
});

// ✅ ALWAYS: Delegate to controller
router.post('/submit', (req, res) => controller.submit(req, res));
```

### 2. All Controllers Extend BaseController

```typescript
class BaseController {
  protected handleSuccess(res: Response, data: unknown, status = 200) {
    res.status(status).json({ success: true, data });
  }

  protected handleError(error: unknown, res: Response, context: string) {
    Sentry.captureException(error, { extra: { context } });
    const statusCode = error instanceof AppError ? error.statusCode : 500;
    res.status(statusCode).json({
      success: false,
      error: { message: error.message, code: error.code }
    });
  }
}

class UserController extends BaseController {
  async getUser(req: Request, res: Response): Promise<void> {
    try {
      const user = await this.userService.findById(req.params.id);
      this.handleSuccess(res, user);
    } catch (error) {
      this.handleError(error, res, 'getUser');
    }
  }
}
```

### 3. All Errors to Sentry

```typescript
try {
  await operation();
} catch (error) {
  Sentry.captureException(error);
  throw error;
}
```

### 4. Use unifiedConfig, NEVER process.env

```typescript
// ❌ NEVER
const timeout = process.env.TIMEOUT_MS;

// ✅ ALWAYS
import { config } from './config/unifiedConfig';
const timeout = config.timeouts.default;
```

### 5. Validate All Input with Zod

```typescript
const schema = z.object({ email: z.string().email() });
const validated = schema.parse(req.body);
```

### 6. Use Repository Pattern for Data Access

```typescript
// Service → Repository → Database
const users = await userRepository.findActive();
```

### 7. Comprehensive Testing Required

```typescript
describe('UserService', () => {
  it('should create user', async () => {
    expect(user).toBeDefined();
  });
});
```

---

## ❌ Anti-Patterns to Avoid

| Anti-Pattern | Correct Approach |
|--------------|------------------|
| Business logic in routes | Delegate to controllers/services |
| Direct `process.env` usage | Use unifiedConfig |
| Missing error handling | Try/catch with Sentry |
| No input validation | Zod schemas for all inputs |
| Direct Prisma everywhere | Repository pattern |
| `console.log` debugging | Sentry + structured logging |
| Any types | Strict TypeScript mode |

---

## 🛡️ BFF Quality Checklist

- [ ] **Validation**: Is every endpoint protected by a Zod schema?
- [ ] **Types**: Is the code free of `any` types and using strict mode?
- [ ] **Caching**: Are frequently accessed data cached with proper TTL?
- [ ] **N+1**: Are database queries optimized to prevent N+1?
- [ ] **Transactions**: Are related mutations wrapped in transactions?
- [ ] **Error Handling**: Is there centralized error handling with proper logging?
- [ ] **Resilience**: Are external calls wrapped with circuit breakers?
- [ ] **Observability**: Are Sentry, OpenTelemetry, and structured logging active?
- [ ] **Security**: Are auth tokens handled via secure header propagation?
- [ ] **Testing**: Combined unit (Vitest) and integration (Supertest) suite?
- [ ] **BaseController**: Do all controllers extend BaseController?
- [ ] **Config**: Is unifiedConfig used instead of process.env?

---
*Last Updated: January 2026*
