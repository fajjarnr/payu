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

    const res = await fetch(`${GATEWAY_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
      signal: AbortSignal.timeout(10_000),
    });

    const data = await res.json();

    if (!res.ok) {
      // Definitive rejection (bad/expired refresh token) ends the session.
      // Anything else (gateway 5xx, timeouts surfaced as !ok) is transient:
      // keep the existing cookies so the client can retry with backoff.
      if (res.status === 401 || res.status === 403 || res.status === 400) {
        logger.warn({ action: 'refresh', status: res.status, durationMs: Date.now() - startTime }, 'Token refresh rejected by gateway');
        const response = NextResponse.json(data, { status: res.status });
        response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'lax' });
        response.cookies.set('refreshToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'lax' });
        return response;
      }
      logger.warn({ action: 'refresh', status: res.status, durationMs: Date.now() - startTime }, 'Token refresh transient failure — preserving session cookies');
      return NextResponse.json(data, { status: res.status });
    }

    const newAccessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const newRefreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    // BUG-CROSS-001: Read expires_in from Keycloak response instead of hardcoding 900s
    const ACCESS_TOKEN_MAX_AGE = data.expires_in ?? data.data?.expires_in ?? 900;

    // BUG-AUTH-035: Rehydrate user data from refresh token response
    let user = data.user ?? data.data?.user;
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
