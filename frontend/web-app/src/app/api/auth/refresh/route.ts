import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://gateway-service:8080';

/**
 * BFF Token Refresh Route — Rotates tokens using the httpOnly refresh cookie.
 *
 * The browser never sees the raw tokens — old refresh token is read from
 * the httpOnly cookie, sent to the backend, and replaced by the new pair.
 */
export async function POST() {
  try {
    const cookieStore = await cookies();
    const refreshToken = cookieStore.get('refreshToken')?.value;

    if (!refreshToken) {
      cookieStore.delete('accessToken');
      return NextResponse.json(
        { success: false, message: 'No refresh token' },
        { status: 401 },
      );
    }

    const res = await fetch(`${GATEWAY_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    const data = await res.json();

    if (!res.ok) {
      cookieStore.delete('accessToken');
      cookieStore.delete('refreshToken');
      return NextResponse.json(data, { status: res.status });
    }

    const newAccessToken =
      data.access_token ?? data.data?.access_token ?? data.data?.accessToken;
    const newRefreshToken =
      data.refresh_token ?? data.data?.refresh_token ?? data.data?.refreshToken;

    const isProduction = process.env.NODE_ENV === 'production';

    if (newAccessToken) {
      cookieStore.set('accessToken', newAccessToken, {
        httpOnly: true,
        secure: isProduction,
        sameSite: 'strict',
        maxAge: 900,
        path: '/',
      });
    }

    if (newRefreshToken) {
      cookieStore.set('refreshToken', newRefreshToken, {
        httpOnly: true,
        secure: isProduction,
        sameSite: 'strict',
        maxAge: 604_800,
        path: '/',
      });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('[BFF] Token refresh error:', error);
    return NextResponse.json(
      { success: false, message: 'Token refresh failed' },
      { status: 503 },
    );
  }
}
