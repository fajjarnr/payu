---
name: cybersecurity-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: []
tags: [security, compliance, zero-trust, vault, jwt, oauth2, rbac]
related: [platform-engineer, data-governance-architect, api-architect]
description: **Master Skill**: Zero Trust Security Architect for PayU. Covers Keycloak (OIDC/SAML), JWT implementation, OAuth2, RBAC, Field Encryption, Secure Coding (OWASP), and Compliance (PCI-DSS).
---

# PayU Cybersecurity Architect Master Skill

You are the **Lead Security Architect** for the **PayU Platform**. You ensure that every component of the digital bank is "Secure by Design" and compliant with **PCI-DSS** and **OJK** regulations.

## ⚡ 2026 Security Priorities
1. **Post-Quantum Cryptography (PQC)**: Preparing for Y2Q by inventorying all crypto assets.
2. **AI-Driven Fraud Detection**: Real-time behavioral biometrics using ML at the edge.
3. **Passkeys First**: Phasing out passwords for customer auth (FIDO2/WebAuthn).
4. **Supply Chain Security**: SLSA Level 3 compliance for all build artifacts (SBOMs).

---

## 🔐 Authentication vs Authorization

### Core Concepts

| Concept | Question | Examples |
|:--------|:---------|:---------|
| **Authentication (AuthN)** | Who are you? | JWT, Sessions, OAuth2, Biometrics |
| **Authorization (AuthZ)** | What can you do? | RBAC, ABAC, Resource ownership |

---

## 🎫 JWT Authentication Implementation

### Pattern 1: Token Generation & Validation

```typescript
// auth/jwt.service.ts
import jwt from 'jsonwebtoken';
import { Request, Response, NextFunction } from 'express';

interface JWTPayload {
  userId: string;
  email: string;
  role: string;
  iat: number;
  exp: number;
}

// Generate Access & Refresh Tokens
function generateTokens(userId: string, email: string, role: string) {
  const accessToken = jwt.sign(
    { userId, email, role },
    process.env.JWT_SECRET!,
    { expiresIn: '15m', algorithm: 'RS256' }  // Short-lived
  );

  const refreshToken = jwt.sign(
    { userId },
    process.env.JWT_REFRESH_SECRET!,
    { expiresIn: '7d' }  // Long-lived
  );

  return { accessToken, refreshToken };
}

// Verify Token Middleware
function verifyToken(token: string): JWTPayload {
  try {
    return jwt.verify(token, process.env.JWT_SECRET!) as JWTPayload;
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new Error('Token expired');
    }
    throw new Error('Invalid token');
  }
}

// Express Middleware
function authenticate(req: Request, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'No token provided' });
  }

  const token = authHeader.substring(7);
  try {
    const payload = verifyToken(token);
    req.user = payload;
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid token' });
  }
}
```

### Pattern 2: Refresh Token Flow

```typescript
// auth/refresh.service.ts
class RefreshTokenService {
  // Store hashed refresh token in database
  async storeRefreshToken(userId: string, refreshToken: string) {
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    await db.refreshTokens.create({
      token: await bcrypt.hash(refreshToken, 12),
      userId,
      expiresAt,
    });
  }

  // Refresh access token
  async refreshAccessToken(refreshToken: string) {
    let payload;
    try {
      payload = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET!) as { userId: string };
    } catch {
      throw new Error('Invalid refresh token');
    }

    // Verify token exists in DB
    const storedToken = await db.refreshTokens.findOne({
      where: {
        userId: payload.userId,
        expiresAt: { $gt: new Date() },
      },
    });

    if (!storedToken || !(await bcrypt.compare(refreshToken, storedToken.token))) {
      throw new Error('Refresh token not found or expired');
    }

    const user = await db.users.findById(payload.userId);
    if (!user) throw new Error('User not found');

    return {
      accessToken: jwt.sign(
        { userId: user.id, email: user.email, role: user.role },
        process.env.JWT_SECRET!,
        { expiresIn: '15m' }
      ),
    };
  }

  // Revoke all tokens (logout all devices)
  async revokeAllUserTokens(userId: string) {
    await db.refreshTokens.deleteMany({ userId });
  }
}
```

---

## 🔑 OAuth2 / Social Login (Passport.js)

```typescript
// auth/oauth.ts
import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';

passport.use(
  new GoogleStrategy(
    {
      clientID: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
      callbackURL: '/api/auth/google/callback',
    },
    async (accessToken, refreshToken, profile, done) => {
      try {
        let user = await db.users.findOne({ googleId: profile.id });

        if (!user) {
          user = await db.users.create({
            googleId: profile.id,
            email: profile.emails?.[0]?.value,
            name: profile.displayName,
          });
        }

        return done(null, user);
      } catch (error) {
        return done(error, undefined);
      }
    }
  )
);

// Routes
app.get('/api/auth/google', passport.authenticate('google', { scope: ['profile', 'email'] }));

app.get('/api/auth/google/callback',
  passport.authenticate('google', { session: false }),
  (req, res) => {
    const tokens = generateTokens(req.user.id, req.user.email, req.user.role);
    res.redirect(`${process.env.FRONTEND_URL}/auth/callback?token=${tokens.accessToken}`);
  }
);
```

---

## 🛡️ Role-Based Access Control (RBAC)

### Pattern 1: Role Hierarchy

```typescript
// auth/rbac.ts
enum Role {
  USER = 'PAYU_USER',
  TELLER = 'PAYU_TELLER',
  ADMIN = 'PAYU_ADMIN',
}

const roleHierarchy: Record<Role, Role[]> = {
  [Role.ADMIN]: [Role.ADMIN, Role.TELLER, Role.USER],
  [Role.TELLER]: [Role.TELLER, Role.USER],
  [Role.USER]: [Role.USER],
};

function hasRole(userRole: Role, requiredRole: Role): boolean {
  return roleHierarchy[userRole]?.includes(requiredRole) ?? false;
}

// Middleware
function requireRole(...roles: Role[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Not authenticated' });
    }

    if (!roles.some(role => hasRole(req.user.role as Role, role))) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }

    next();
  };
}

// Usage
app.delete('/api/users/:id', authenticate, requireRole(Role.ADMIN), async (req, res) => {
  await db.users.delete(req.params.id);
  res.json({ message: 'User deleted' });
});
```

### Pattern 2: Permission-Based Access

```typescript
// auth/permissions.ts
enum Permission {
  READ_ACCOUNTS = 'read:accounts',
  WRITE_ACCOUNTS = 'write:accounts',
  TRANSFER = 'transfer:funds',
  VIEW_REPORTS = 'view:reports',
  ADMIN_USERS = 'admin:users',
}

const rolePermissions: Record<Role, Permission[]> = {
  [Role.USER]: [Permission.READ_ACCOUNTS, Permission.TRANSFER],
  [Role.TELLER]: [Permission.READ_ACCOUNTS, Permission.WRITE_ACCOUNTS, Permission.TRANSFER, Permission.VIEW_REPORTS],
  [Role.ADMIN]: Object.values(Permission),
};

function requirePermission(...permissions: Permission[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) return res.status(401).json({ error: 'Not authenticated' });

    const userPermissions = rolePermissions[req.user.role as Role] ?? [];
    const hasAllPermissions = permissions.every(p => userPermissions.includes(p));

    if (!hasAllPermissions) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }

    next();
  };
}

// Usage
app.post('/api/transfers', authenticate, requirePermission(Permission.TRANSFER), async (req, res) => {
  // Transfer logic
});
```

### Pattern 3: Resource Ownership

```typescript
// auth/ownership.ts
function requireOwnership(resourceType: 'account' | 'transaction', idParam = 'id') {
  return async (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) return res.status(401).json({ error: 'Not authenticated' });

    const resourceId = req.params[idParam];

    // Admins can access anything
    if (req.user.role === Role.ADMIN) return next();

    let resource;
    if (resourceType === 'account') {
      resource = await db.accounts.findById(resourceId);
    } else if (resourceType === 'transaction') {
      resource = await db.transactions.findById(resourceId);
    }

    if (!resource) return res.status(404).json({ error: 'Resource not found' });
    if (resource.userId !== req.user.userId) {
      return res.status(403).json({ error: 'Not authorized' });
    }

    next();
  };
}

// Usage - User can only view their own accounts
app.get('/api/accounts/:id', authenticate, requireOwnership('account'), async (req, res) => {
  const account = await db.accounts.findById(req.params.id);
  res.json({ account });
});
```

---

## 🔒 Password Security

```typescript
// auth/password.ts
import bcrypt from 'bcrypt';
import { z } from 'zod';

// Strong password validation
const passwordSchema = z.string()
  .min(12, 'Password must be at least 12 characters')
  .regex(/[A-Z]/, 'Must contain uppercase letter')
  .regex(/[a-z]/, 'Must contain lowercase letter')
  .regex(/[0-9]/, 'Must contain number')
  .regex(/[^A-Za-z0-9]/, 'Must contain special character');

async function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, 12);  // 2^12 iterations
}

async function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

// Registration with validation
app.post('/api/auth/register', async (req, res) => {
  try {
    const { email, password, phone } = req.body;

    passwordSchema.parse(password);

    const existingUser = await db.users.findOne({ email });
    if (existingUser) {
      return res.status(400).json({ error: 'Email already registered' });
    }

    const passwordHash = await hashPassword(password);
    const user = await db.users.create({ email, passwordHash, phone });
    const tokens = generateTokens(user.id, user.email, user.role);

    res.status(201).json({ user: { id: user.id, email: user.email }, ...tokens });
  } catch (error) {
    if (error instanceof z.ZodError) {
      return res.status(400).json({ error: error.errors[0].message });
    }
    res.status(500).json({ error: 'Registration failed' });
  }
});
```

---

## ⏱️ Rate Limiting

```typescript
// security/rate-limit.ts
import rateLimit from 'express-rate-limit';
import RedisStore from 'rate-limit-redis';

// Strict limiter for auth endpoints (prevent brute force)
const authLimiter = rateLimit({
  store: new RedisStore({ client: redisClient }),
  windowMs: 15 * 60 * 1000,  // 15 minutes
  max: 5,  // 5 attempts
  message: { error: 'Too many login attempts, please try again later' },
  standardHeaders: true,
  legacyHeaders: false,
});

// General API rate limiter
const apiLimiter = rateLimit({
  windowMs: 60 * 1000,  // 1 minute
  max: 100,  // 100 requests per minute
  standardHeaders: true,
});

// Transfer limiter (prevent fund draining)
const transferLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,  // 1 hour
  max: 10,  // 10 transfers per hour
  message: { error: 'Transfer limit reached, please try again later' },
});

// Apply to routes
app.post('/api/auth/login', authLimiter, loginHandler);
app.post('/api/transfers', authenticate, transferLimiter, transferHandler);
app.use('/api/', apiLimiter);
```

---

## 🍪 Session-Based Authentication (Alternative)

```typescript
// auth/session.ts
import session from 'express-session';
import RedisStore from 'connect-redis';

app.use(session({
  store: new RedisStore({ client: redisClient }),
  secret: process.env.SESSION_SECRET!,
  resave: false,
  saveUninitialized: false,
  cookie: {
    secure: process.env.NODE_ENV === 'production',  // HTTPS only
    httpOnly: true,  // No JavaScript access
    maxAge: 24 * 60 * 60 * 1000,  // 24 hours
    sameSite: 'strict',  // CSRF protection
  },
}));

// Login
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  const user = await db.users.findOne({ email });

  if (!user || !(await verifyPassword(password, user.passwordHash))) {
    return res.status(401).json({ error: 'Invalid credentials' });
  }

  req.session.userId = user.id;
  req.session.role = user.role;
  res.json({ user: { id: user.id, email: user.email } });
});

// Logout
app.post('/api/auth/logout', (req, res) => {
  req.session.destroy((err) => {
    if (err) return res.status(500).json({ error: 'Logout failed' });
    res.clearCookie('connect.sid');
    res.json({ message: 'Logged out successfully' });
  });
});
```

---

## 🛡️ Secure Coding & Data Protection

### 1. Field-Level Encryption & Masking
- **Encryption at Rest**: PII (NIK, Card Number) MUST be encrypted before hitting the database using `security-starter`.
- **Log Masking**: Use `@Sensitive` in Java or RegEx in Python to prevent PII leakage in Loki/Jaeger.

### 2. OWASP Guardrails
- **Input Sanitization**: Use Pydantic/Zod/Bean Validation for all external data.
- **Secure Headers**: Always set `Content-Security-Policy`, `X-Frame-Options`, and `Strict-Transport-Security`.

```typescript
// security/headers.ts
app.use((req, res, next) => {
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});
```

---

## 🏗️ K8s Security (Zero Trust)

- **Network Policies**: Deny-all by default. Only allow specific Pod-to-Pod traffic.
- **Secrets Management**: Use **HashiCorp Vault** or OpenShift Secrets. NEVER hardcode keys.
- **Container Hardening**: Use UBI9-minimal images. Run as non-root user.

### 4. Fraud Detection Rules (Compliance @OJK)

Setiap transaksi harus melewati *Deterministic Rule Engine* sebelum dinyatakan valid.

#### Velocity Checks (Rate of Transaction)

```typescript
// patterns/fraud-rules.ts
export const fraudRules = [
  {
    id: "VELOCITY_1MIN",
    condition: (txs) => txs.filter(t => t.timestamp > Date.now() - 60000).length > 3,
    action: "BLOCK",
    reason: "Too many transactions in 1 minute"
  },
  {
    id: "MIDNIGHT_SURGE",
    condition: (txs, currentTx) => {
        const hour = new Date(currentTx.timestamp).getHours();
        return (hour >= 0 && hour <= 4) && currentTx.amount > 5000000;
    },
    action: "CHALLENGE_MFA",
    reason: "High value transaction during sleeping hours"
  }
];
```

#### Geo-Fencing (Impossible Travel)
Jika user login di Jakarta jam 10:00, lalu login di Russia jam 10:05 -> **BLOCK**.

### 5. Audit Evidence (POJK 12/2017)

Data yang wajib tersedia saat audit OJK:

1.  **Audit Trail**: `who`, `what`, `when`, `where` (IP/Location), `status` (Success/Fail).
2.  **Change Management**: Bukti approval PR untuk setiap deployment ke Production.
3.  **Access Review**: Laporan bulanan user yang punya akses ke Production DB (harus 0 atau *Just-In-Time*).

---

---

## ⚠️ Security Best Practices

| Practice | Implementation |
|:---------|:---------------|
| **Never Store Plain Passwords** | Always hash with bcrypt (12+ rounds) |
| **Use HTTPS** | Encrypt data in transit, TLS 1.3 |
| **Short-Lived Access Tokens** | < 15 minutes |
| **Secure Cookies** | httpOnly, secure, sameSite flags |
| **Validate All Input** | Zod/Pydantic for all external data |
| **Rate Limit Auth Endpoints** | Prevent brute force |
| **CSRF Protection** | SameSite cookies, CSRF tokens |
| **Rotate Secrets** | JWT secrets, API keys regularly |
| **Log Security Events** | Login attempts, failed auth |
| **Use MFA** | Mandatory for financial operations |

---

## 🚫 Common Pitfalls

| Pitfall | Solution |
|:--------|:---------|
| JWT in localStorage | Use httpOnly cookies (XSS vulnerable) |
| No token expiration | Always set exp claim |
| Client-side auth only | Always validate server-side |
| Insecure password reset | Use secure tokens with expiration |
| No rate limiting | Vulnerable to brute force |
| Trusting client data | Always validate on server |

---

## 📚 References

### Local Reference Files

| Category | Topic | File |
|----------|-------|------|
| **Compliance** | SOC2, ISO27001, GDPR, HIPAA, PCI-DSS frameworks | [compliance-frameworks.md](./references/compliance-frameworks.md) |
| **Architecture** | Zero Trust, Defense-in-Depth, IAM design | [security-architecture.md](./references/security-architecture.md) |
| **Operations** | SOC, incident response, threat hunting | [security-operations.md](./references/security-operations.md) |
| **Threat Modeling** | STRIDE, PASTA, risk assessment | [threat-modeling-risk.md](./references/threat-modeling-risk.md) |
| **AppSec** | OWASP Top 10, secure coding, SAST/DAST | [application-security.md](./references/application-security.md) |
| **K8s Hardening** | OpenShift/Kubernetes security baselines | [k8s-hardening.md](./references/k8s-hardening.md) |
| **Secrets** | External Secrets Operator, Vault integration | [external-secrets.md](./references/external-secrets.md) |

### External Documentation

- [OWASP Top 10](https://owasp.org/Top10/)
- [OWASP Application Security Verification Standard](https://owasp.org/www-project-application-security-verification-standard/)
- [PCI-DSS Requirements](https://www.pcisecuritystandards.org/document_library)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks)
- [HashiCorp Vault](https://www.vaultproject.io/docs)
- [Red Hat SSO (Keycloak)](https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/)

---
*Last Updated: January 2026*
