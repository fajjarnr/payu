import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import logger from '@/lib/logger';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

/**
 * BFF Logout Route — Clears httpOnly auth cookies and notifies backend.
 */
export async function POST() {
  try {
    const cookieStore = await cookies();
    const refreshToken = cookieStore.get('refreshToken')?.value;

    logger.info({ action: 'logout', hasRefreshToken: !!refreshToken }, 'Logout initiated');

    // LOGIN-002: revoke the session at the identity provider with the refresh
    // token (OIDC end_session) so the token cannot be replayed after logout.
    if (refreshToken) {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2000);
      try {
        await fetch(`${GATEWAY_URL}/api/v1/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refresh_token: refreshToken }),
          signal: controller.signal,
        });
      } catch (err) {
        logger.warn({ action: 'logout', err: err instanceof Error ? err : { message: String(err) } }, 'Backend logout notification failed');
      } finally {
        clearTimeout(timeoutId);
      }
    }

    // BUG-AUTH-027: Set secure cookie flags when clearing
    const isProduction = process.env.NODE_ENV === 'production';
    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isProduction, sameSite: 'strict' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isProduction, sameSite: 'strict' });

    logger.info({ action: 'logout' }, 'Logout successful — cookies cleared');
    return response;
  } catch (error) {
    logger.error({ action: 'logout', err: error instanceof Error ? error : { message: String(error) } }, 'Logout error — clearing cookies anyway');
    // Even on error, clear cookies
    const isProduction = process.env.NODE_ENV === 'production';
    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isProduction, sameSite: 'strict' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/', httpOnly: true, secure: isProduction, sameSite: 'strict' });
    return response;
  }
}
