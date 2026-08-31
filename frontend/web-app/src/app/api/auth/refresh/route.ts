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
      response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'strict' });
      return response;
    }

    logger.info({ action: 'refresh' }, 'Token refresh attempt');

    const res = await fetch(`${GATEWAY_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    const data = await res.json();

    if (!res.ok) {
      logger.warn({ action: 'refresh', status: res.status, durationMs: Date.now() - startTime }, 'Token refresh rejected by gateway');
      const response = NextResponse.json(data, { status: res.status });
      response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'strict' });
      response.cookies.set('refreshToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'strict' });
      return response;
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
        sameSite: 'strict',
        maxAge: ACCESS_TOKEN_MAX_AGE,
        path: '/',
      });
    }

    if (newRefreshToken) {
      response.cookies.set('refreshToken', newRefreshToken, {
        httpOnly: true,
        secure: isSecure,
        sameSite: 'strict',
        maxAge: 604_800,
        path: '/',
      });
    }

    logger.info({ action: 'refresh', durationMs: Date.now() - startTime }, 'Token refresh successful');

    return response;
  } catch (error) {
    logger.error({ action: 'refresh', err: error instanceof Error ? error : { message: String(error) }, durationMs: Date.now() - startTime }, 'Token refresh proxy error');
    // BUG-AUTH-014: Clear stale cookies on all error paths to prevent
    // infinite refresh loops when the gateway is unreachable.
    const response = NextResponse.json(
      { success: false, message: 'Token refresh failed' },
      { status: 503 },
    );
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'strict' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isSecure, sameSite: 'strict' });
    return response;
  }
}
