import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

/**
 * BFF Logout Route — Clears httpOnly auth cookies and notifies backend.
 */
export async function POST() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('accessToken')?.value;

    // Best-effort notify backend to invalidate session
    if (token) {
      fetch(`${GATEWAY_URL}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {
        /* fire-and-forget */
      });
    }

    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/' });
    return response;
  } catch {
    // Even on error, clear cookies
    const response = NextResponse.json({ success: true });
    response.cookies.set('accessToken', '', { maxAge: 0, path: '/' });
    response.cookies.set('refreshToken', '', { maxAge: 0, path: '/' });
    return response;
  }
}
