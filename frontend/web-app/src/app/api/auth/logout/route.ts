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

    cookieStore.delete('accessToken');
    cookieStore.delete('refreshToken');

    return NextResponse.json({ success: true });
  } catch {
    // Even on error, clear cookies
    const cookieStore = await cookies();
    cookieStore.delete('accessToken');
    cookieStore.delete('refreshToken');
    return NextResponse.json({ success: true });
  }
}
