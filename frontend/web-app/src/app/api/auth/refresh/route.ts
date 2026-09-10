import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import logger from '@/lib/logger';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

/**
 * Decode JWT payload without verifying signature (BFF already trusts the token from the gateway).
 * Extracts user claims from the Keycloak access token.
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = Buffer.from(parts[1], 'base64url').toString('utf-8');
    return JSON.parse(payload);
  } catch (err) {
    console.error('[refresh] JWT decode failed:', err);
    return null;
  }
}

/**
 * BFF Token Refresh Route — Rotates tokens using the httpOnly refresh cookie.
 *
 * The browser never sees the raw tokens — old refresh token is read from
 * the httpOnly cookie, sent to the backend, and replaced by the new pair.
 */
/**
 * FE-AUDIT-001: single-flight per refresh cookie. Keycloak refresh tokens
 * are single-use — N concurrent rotations (BFF proxy retry + middleware
 * rehydration + client timer + axios queue) burn N-1 into invalid_grant
 * logout. Concurrent callers sharing one cookie await the same upstream
 * rotation and each receive the same new pair.
 * ponytail: per-pod memory map. Distributed lock (Redis) only if
 * multi-replica rotation collisions observed in metrics.
 */
const inflightRotations = new Map<string, Promise<RotationResult>>();
interface RotationResult {
  status: number;
  data: Record<string, unknown>;
}

function recordOf(value: unknown): Record<string, unknown> | undefined {
  return typeof value === 'object' && value !== null
    ? (value as Record<string, unknown>)
    : undefined;
}

function firstString(...candidates: unknown[]): string | undefined {
  for (const candidate of candidates) {
    if (typeof candidate === 'string' && candidate.length > 0) return candidate;
  }
  return undefined;
}

async function performRotation(refreshToken: string): Promise<RotationResult> {
  const res = await fetch(`${GATEWAY_URL}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh_token: refreshToken }),
    signal: AbortSignal.timeout(10_000),
  });
  // Passthrough proxy: unknown gateway fields forwarded verbatim below; consumers narrow per field.
  const data = (await res.json()) as unknown as Record<string, unknown>;
  return { status: res.status, data };
}

function rotateSingleFlight(refreshToken: string): Promise<RotationResult> {
  const existing = inflightRotations.get(refreshToken);
  if (existing) return existing;
  const rotation = performRotation(refreshToken).finally(() => {
    if (inflightRotations.get(refreshToken) === rotation) {
      inflightRotations.delete(refreshToken);
    }
  });
  inflightRotations.set(refreshToken, rotation);
  return rotation;
}

export async function POST() {
  const startTime = Date.now();
  const isSecure = (process.env.NEXT_PUBLIC_BASE_URL ?? "").startsWith("https://");
  try {
    const cookieStore = await cookies();
    const refreshToken = cookieStore.get('refreshToken')?.value;

    if (!refreshToken) {
      logger.debug({ action: 'refresh' }, 'Token refresh skipped — no refresh token cookie');
      const response = NextResponse.json(
        { success: false, message: 'No refresh token' },
        { status: 401 },
      );
      response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'lax' });
      return response;
    }

    logger.info({ action: 'refresh' }, 'Token refresh attempt');

    // FE-AUDIT-001: concurrent same-cookie callers share one rotation.
    const { status, data } = await rotateSingleFlight(refreshToken);

    if (status < 200 || status >= 300) {
      // NEVER wipe cookies here — not even on 401/403/400. Keycloak reports
      // rotation races and revoked grants alike as 400 invalid_grant, and a
      // concurrent refresh (client timer + middleware rehydration sharing one
      // single-use token) routinely loses: wiping turns that race into a
      // forced logout. Stale cookies expire on their own; an actually-dead
      // session simply fails validation and lands on login, where SSO
      // silently re-authenticates while the IdP session lives.
      logger.warn({ action: 'refresh', status, durationMs: Date.now() - startTime }, 'Token refresh rejected — preserving session cookies');
      return NextResponse.json(data, { status });
    }

    const nested = recordOf(data.data);
    const newAccessToken = firstString(data.access_token, nested?.access_token, nested?.accessToken);
    const newRefreshToken = firstString(data.refresh_token, nested?.refresh_token, nested?.refreshToken);

    // BUG-CROSS-001: Read expires_in from Keycloak response instead of hardcoding 900s
    const expiresRaw = typeof data.expires_in === 'number'
      ? data.expires_in
      : typeof nested?.expires_in === 'number' ? nested.expires_in : 900;
    const ACCESS_TOKEN_MAX_AGE = expiresRaw;

    // BUG-AUTH-035: Rehydrate user data from refresh token response
    let user: unknown = data.user ?? nested?.user;
    if (!user && newAccessToken) {
      const claims = decodeJwtPayload(newAccessToken);
      if (claims) {
        const accountId = (claims.account_id as string) || `account-${claims.sub}`;
        user = {
          id: claims.sub as string,
          accountId,
          username: claims.preferred_username as string,
          fullName: (claims.name as string) || '',
          email: (claims.email as string) || '',
          roles:
            ((claims.realm_access as Record<string, unknown>)?.roles as string[]) || [],
        };
      }
    }

    // BUG-AUTH-005: Only return expiresIn if newAccessToken was actually received
    const response = NextResponse.json({
      success: true,
      ...(newAccessToken ? { expiresIn: ACCESS_TOKEN_MAX_AGE } : {}),
      ...(user ? { user } : {}),
    });

    if (newAccessToken) {
      response.cookies.set('accessToken', newAccessToken, {
        httpOnly: true,
        secure: isSecure,
        sameSite: 'lax',
        maxAge: ACCESS_TOKEN_MAX_AGE,
        path: '/',
      });
    }

    if (newRefreshToken) {
      response.cookies.set('refreshToken', newRefreshToken, {
        httpOnly: true,
        secure: isSecure,
        sameSite: 'lax',
        maxAge: 604_800,
        path: '/',
      });
    }

    logger.info({ action: 'refresh', durationMs: Date.now() - startTime }, 'Token refresh successful');

    return response;
  } catch (error) {
    // Transient (network/timeout): NEVER clear cookies here. The existing
    // tokens are still valid until expiry and the client retries with
    // backoff — wiping them turns a blip into a forced logout.
    logger.error({ action: 'refresh', err: error instanceof Error ? error : { message: String(error) }, durationMs: Date.now() - startTime }, 'Token refresh proxy error — session preserved');
    return NextResponse.json(
      { success: false, message: 'Token refresh failed' },
      { status: 503 },
    );
  }
}
