import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import logger from '@/lib/logger';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

/**
 * BFF Token Refresh Route — Rotates tokens using the httpOnly refresh cookie.
 *
 * The browser never sees the raw tokens — old refresh token is read from
 * the httpOnly cookie, sent to the backend, and replaced by the new pair.
 */
export async function POST() {
  const startTime = Date.now();
  try {
    const cookieStore = await cookies();
    const refreshToken = cookieStore.get('refreshToken')?.value;

    if (!refreshToken) {
      logger.warn({ action: 'refresh' }, 'Token refresh failed — no refresh token cookie');
      const response = NextResponse.json(
        { success: false, message: 'No refresh token' },
        { status: 401 },
      );
      response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
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
      response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
      response.cookies.set('refreshToken', '', { maxAge: 0, path: '/' });
      return response;
    }

    const newAccessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const newRefreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    // BUG-CROSS-001: Read expires_in from Keycloak response instead of hardcoding 900s
    const ACCESS_TOKEN_MAX_AGE = data.expires_in ?? data.data?.expires_in ?? 900;
    // BUG-AUTH-005: Only return expiresIn if newAccessToken was actually received
    const response = NextResponse.json({
      success: true,
      ...(newAccessToken ? { expiresIn: ACCESS_TOKEN_MAX_AGE } : {}),
    });

    if (newAccessToken) {
      response.cookies.set('accessToken', newAccessToken, {
        httpOnly: true,
        secure: false, // Labs environment: relax secure requirement
        sameSite: 'lax',
        maxAge: ACCESS_TOKEN_MAX_AGE,
        path: '/',
      });
    }

    if (newRefreshToken) {
      response.cookies.set('refreshToken', newRefreshToken, {
        httpOnly: true,
        secure: false, // Labs environment: relax secure requirement
        sameSite: 'lax',
        maxAge: 604_800,
        path: '/',
      });
    }

    logger.info({ action: 'refresh', durationMs: Date.now() - startTime }, 'Token refresh successful');

    return response;
  } catch (error) {
    logger.error({ action: 'refresh', err: error instanceof Error ? error : { message: String(error) }, durationMs: Date.now() - startTime }, 'Token refresh proxy error');
    return NextResponse.json(
      { success: false, message: 'Token refresh failed' },
      { status: 503 },
    );
  }
}
