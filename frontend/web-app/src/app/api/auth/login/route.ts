import { NextResponse } from "next/server";
import logger, { getCorrelationId, withCorrelation } from "@/lib/logger"; // eslint-disable-line @typescript-eslint/no-unused-vars

const GATEWAY_URL = process.env.GATEWAY_URL || "https://gateway-service:8080";

// AUDIT-071: Rate limiting for login endpoint (5 attempts per 5 minutes per IP)
const RATE_LIMIT_MAX = 5;
const RATE_LIMIT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
const loginAttempts = new Map<string, { count: number; resetTime: number }>();

function getClientIp(request: Request): string {
  const forwarded = request.headers.get("x-forwarded-for");
  if (forwarded) return forwarded.split(",")[0].trim();
  return request.headers.get("x-real-ip") ?? "unknown";
}

function checkRateLimit(ip: string): boolean {
  const now = Date.now();
  const record = loginAttempts.get(ip);
  if (!record || now > record.resetTime) {
    loginAttempts.set(ip, { count: 1, resetTime: now + RATE_LIMIT_WINDOW_MS });
    return true;
  }
  record.count++;
  return record.count <= RATE_LIMIT_MAX;
}

// Periodic cleanup to prevent memory growth (every 10 minutes)
setInterval(() => {
  const now = Date.now();
  for (const [ip, record] of loginAttempts) {
    if (now > record.resetTime) loginAttempts.delete(ip);
  }
}, 10 * 60 * 1000);

/**
 * Decode JWT payload without verifying signature.
 *
 * AUDIT-072 DECISION: BFF trusts gateway response — JWT signature verification
 * happens at gateway-service (Quarkus OIDC filter + Keycloak JWKS). BFF→gateway
 * runs inside cluster network. When mTLS enforced (GAP-8/OCP-007), this path is
 * fully secured. Duplicating sig verification here adds latency + JWKS fetch
 * dependency with zero security gain (BFF never receives tokens from untrusted source).
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = Buffer.from(parts[1], "base64url").toString("utf-8");
    return JSON.parse(payload);
  } catch (err) {
    console.error('[login] JWT decode failed:', err);
    return null;
  }
}

/**
 * BFF Login Route — Authenticates user and stores tokens in httpOnly cookies.
 *
 * Flow:
 *   Browser → POST /api/auth/login → this route → gateway /api/v1/auth/login
 *   Returns user data only; tokens are NEVER exposed to JavaScript.
 *
 * Security:
 *   - httpOnly: JS cannot read tokens (prevents XSS token theft)
 *   - secure: HTTPS-only in production
 *   - sameSite=lax: allows same-site navigation while preventing CSRF
 *   - PCI-DSS 8.2.4 compliant
 *   - AUDIT-071: Rate limited (5 attempts/5min per IP)
 */
export async function POST(request: Request) {
  const correlationId = getCorrelationId(request);
  const log = withCorrelation(correlationId);
  const startTime = Date.now();

  // AUDIT-071: Check rate limit before processing
  const clientIp = getClientIp(request);
  if (!checkRateLimit(clientIp)) {
    log.warn({ action: "login", ip: clientIp }, "Rate limit exceeded");
    return NextResponse.json(
      { success: false, message: "Too many login attempts. Please try again later." },
      { status: 429, headers: { "Retry-After": "300" } },
    );
  }

  try {
    const body = await request.json();
    log.info({ action: "login" }, "Login attempt");

    const res = await fetch(`${GATEWAY_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await res.json();

    if (!res.ok) {
      log.warn({ action: "login", status: res.status, durationMs: Date.now() - startTime }, "Login failed");
      return NextResponse.json(data, { status: res.status });
    }

    // Backend may return tokens at top-level or nested in data
    const accessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const refreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    // Extract user info from JWT payload (auth-service returns Keycloak tokens directly)
    let user = data.user ?? data.data?.user;
    if (!user && accessToken) {
      const claims = decodeJwtPayload(accessToken);
      if (claims) {
        // BUG-CROSS-033 FIX: Extract account_id from JWT claim (same logic as gateway AuthorizationFilter)
        // Gateway uses 'account_id' claim, falling back to 'account-' + sub
        const accountId = (claims.account_id as string) || `account-${claims.sub}`;
        user = {
          id: claims.sub as string,
          accountId, // separate from user id
          username: claims.preferred_username as string,
          fullName: (claims.name as string) || "",
          email: (claims.email as string) || "",
          roles:
            ((claims.realm_access as Record<string, unknown>)
              ?.roles as string[]) || [],
        };
      }
    }

    const isProduction = process.env.NODE_ENV === "production";

    // Build response and set cookies directly on the NextResponse object
    // (cookies() from next/headers does NOT attach Set-Cookie to NextResponse.json())
    // BUG-CROSS-005: Use expires_in from Keycloak response instead of hardcoded 900s
    const ACCESS_TOKEN_MAX_AGE = data.expires_in ?? data.data?.expires_in ?? 900;
    const responseData = {
      ...(data.data || data),
      user: user || data.data?.user,
      expiresIn: ACCESS_TOKEN_MAX_AGE, // seconds until accessToken expires — safe to expose
    };
    const response = NextResponse.json({ success: true, data: responseData });
    response.headers.set("X-Correlation-Id", correlationId);

    if (accessToken) {
      response.cookies.set("accessToken", accessToken, {
        httpOnly: true,
        secure: isProduction, // BUG-AUTH-027: HTTPS-only in production
        sameSite: "strict", // BUG-AUTH-027: strict to prevent CSRF
        maxAge: ACCESS_TOKEN_MAX_AGE,
        path: "/",
      });
    }

    if (refreshToken) {
      response.cookies.set("refreshToken", refreshToken, {
        httpOnly: true,
        secure: isProduction, // BUG-AUTH-027: HTTPS-only in production
        sameSite: "strict", // BUG-AUTH-027: strict to prevent CSRF
        maxAge: 604_800, // 7 days
        path: "/",
      });
    }

    log.info({ action: "login", userId: user?.id, durationMs: Date.now() - startTime }, "Login successful");

    return response;
  } catch (error) {
    log.error({ action: "login", err: error instanceof Error ? error : { message: String(error) }, durationMs: Date.now() - startTime }, "Login proxy error");
    return NextResponse.json(
      { success: false, message: "Authentication service unavailable" },
      { status: 503 },
    );
  }
}
