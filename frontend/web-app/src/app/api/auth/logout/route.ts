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
    const token = cookieStore.get('accessToken')?.value;

    logger.info({ action: 'logout', hasToken: !!token }, 'Logout initiated');

    // BUG-FE-014: Await backend logout with 2s timeout to invalidate session before clearing cookies
    if (token) {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2000);
      try {
        await fetch(`${GATEWAY_URL}/api/v1/auth/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
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
