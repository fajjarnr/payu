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

    // Best-effort notify backend to invalidate session
    if (token) {
      fetch(`${GATEWAY_URL}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }).catch((err) => {
        logger.warn({ action: 'logout', err: err instanceof Error ? err : { message: String(err) } }, 'Backend logout notification failed');
      });
    }

    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/' });

    logger.info({ action: 'logout' }, 'Logout successful — cookies cleared');
    return response;
  } catch (error) {
    logger.error({ action: 'logout', err: error instanceof Error ? error : { message: String(error) } }, 'Logout error — clearing cookies anyway');
    // Even on error, clear cookies
    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/' });
    return response;
  }
}
